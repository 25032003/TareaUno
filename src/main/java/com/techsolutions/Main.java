package com.techsolutions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

    static int totalProcesos = 0;
    static int procesosCompletos = 0;
    static int procesosPendientes = 0;
    static int recursosHerramienta = 0;
    static double sumaEficiencia = 0;
    static int procesosContados = 0;
    static JSONObject procesoMasAntiguo = null;

    public static void main(String[] args) throws IOException, InterruptedException {
        String apiUrl = "https://58o1y6qyic.execute-api.us-east-1.amazonaws.com/default/taskReport";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Mostrar JSON recibido para verificar campos
        System.out.println("===== RESPUESTA COMPLETA DEL API GENERADOR =====");
        System.out.println(response.body());
        System.out.println("================================================");

        JSONObject json = new JSONObject(response.body());

        JSONObject resultado = construirResultado(json);
        enviarEvaluacion(resultado, client);
    }

    static void procesar(JSONObject proceso) {
        totalProcesos++;

        String estado = proceso.optString("estado", "").toLowerCase();
        if (estado.equals("completo")) {
            procesosCompletos++;
        } else {
            procesosPendientes++;
        }

        if (proceso.has("recursos")) {
            JSONArray recursos = proceso.getJSONArray("recursos");
            for (int i = 0; i < recursos.length(); i++) {
                JSONObject recurso = recursos.getJSONObject(i);
                String tipo = recurso.optString("tipo", "");
                if (tipo.equalsIgnoreCase("herramienta")) {
                    recursosHerramienta++;
                }
            }
        }

        if (proceso.has("eficiencia")) {
            sumaEficiencia += proceso.getDouble("eficiencia");
            procesosContados++;
        }

        if (proceso.has("fechaInicio")) {
            String fecha = proceso.getString("fechaInicio");
            if (procesoMasAntiguo == null || fecha.compareTo(procesoMasAntiguo.getString("fechaInicio")) < 0) {
                procesoMasAntiguo = new JSONObject();
                procesoMasAntiguo.put("id", proceso.get("id"));
                procesoMasAntiguo.put("fechaInicio", fecha);
            }
        }

        if (proceso.has("subprocesos")) {
            JSONArray hijos = proceso.getJSONArray("subprocesos");
            for (int i = 0; i < hijos.length(); i++) {
                procesar(hijos.getJSONObject(i));
            }
        }
    }

    static JSONObject construirResultado(JSONObject jsonCompleto) {
        JSONArray procesos = jsonCompleto.getJSONArray("procesos");

        for (int i = 0; i < procesos.length(); i++) {
            procesar(procesos.getJSONObject(i));
        }

        double eficienciaPromedio = procesosContados > 0 ? sumaEficiencia / procesosContados : 0;

        JSONObject resultado = new JSONObject();
        resultado.put("nombre", "Brandon Stiven Lopez");
        resultado.put("carnet", "0905-22-8848");
        resultado.put("seccion", "A");

        JSONObject resumen = new JSONObject();
        resumen.put("totalProcesos", totalProcesos);
        resumen.put("procesosCompletos", procesosCompletos);
        resumen.put("procesosPendientes", procesosPendientes);
        resumen.put("recursosTipoHerramienta", recursosHerramienta);
        resumen.put("eficienciaPromedio", eficienciaPromedio);
        resumen.put("procesoMasAntiguo", procesoMasAntiguo != null ? procesoMasAntiguo : JSONObject.NULL);

        resultado.put("resultadoBusqueda", resumen);
        resultado.put("payload", jsonCompleto); // JSON completo con "auditor" y "metadata"

        return resultado;
    }

    static void enviarEvaluacion(JSONObject resultado, HttpClient client) throws IOException, InterruptedException {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://t199qr74fg.execute-api.us-east-1.amazonaws.com/default/taskReportVerification"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(resultado.toString()))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Respuesta del evaluador:");
        System.out.println(postResponse.body());
    }
}
