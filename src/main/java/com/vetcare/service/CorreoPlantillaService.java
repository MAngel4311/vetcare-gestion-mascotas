package com.vetcare.service;

/**
 * Convierte el texto de un correo en un mensaje HTML con la imagen de Vet-Care.
 *
 * Los servicios siguen armando el mensaje como texto simple. Esta clase solo lo presenta:
 *  - los párrafos normales se muestran como texto;
 *  - un párrafo donde todas las líneas empiezan con dos espacios («  Fecha: ...») se muestra
 *    como una tarjeta de datos, con la etiqueta a la izquierda y el valor a la derecha;
 *  - el cierre «Vet-Care» se omite, porque el pie del correo ya lo incluye.
 *
 * Usa estilos en línea y tablas porque los programas de correo (Gmail, Outlook) ignoran casi todo el CSS moderno.
 */
final class CorreoPlantilla {

    private static final String TINTA = "#1f3a5f";
    private static final String TEAL = "#1a8fa8";
    private static final String VERDE = "#3db6a4";
    private static final String FONDO = "#eef4f3";
    private static final String LINEA = "#d3e1df";
    private static final String GRIS = "#5b7371";
    private static final String FUENTE = "font-family:'Segoe UI',Helvetica,Arial,sans-serif;";

    private CorreoPlantilla() {
    }

    /** @param conLogo si es true usa la imagen incrustada «cid:logo»; si no, muestra el nombre en texto. */
    static String html(String texto, boolean conLogo) {
        StringBuilder cuerpo = new StringBuilder();
        for (String parrafo : texto.strip().split("\\R\\s*\\R")) {
            String[] lineas = parrafo.split("\\R");
            if (parrafo.strip().equals("Vet-Care") || parrafo.strip().equals("Vet-Care.")) {
                continue; // el pie ya firma el mensaje
            }
            if (esTarjetaDeDatos(lineas)) {
                cuerpo.append(tarjeta(lineas));
            } else {
                cuerpo.append("<p style=\"margin:0 0 16px 0;font-size:16px;line-height:1.6;color:").append(TINTA)
                        .append(";\">").append(texto(lineas)).append("</p>");
            }
        }

        String encabezado = conLogo
                ? "<img src=\"cid:logo\" alt=\"Vet-Care\" width=\"190\" style=\"display:block;margin:0 auto;width:190px;height:auto;border:0;\">"
                : "<div style=\"font-size:30px;font-weight:700;color:" + TEAL + ";text-align:center;" + FUENTE + "\">Vet-Care</div>";

        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:" + FONDO + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + FONDO + ";\">"
                + "<tr><td align=\"center\" style=\"padding:28px 12px;\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"width:100%;max-width:600px;background:#ffffff;border:1px solid " + LINEA + ";border-radius:12px;overflow:hidden;" + FUENTE + "\">"
                // franja de color de la marca
                + "<tr><td style=\"height:6px;line-height:6px;font-size:0;background:" + TEAL + ";"
                + "background-image:linear-gradient(90deg,#2a7fd4," + VERDE + ");\">&nbsp;</td></tr>"
                // logo
                + "<tr><td style=\"padding:26px 28px 8px 28px;\">" + encabezado + "</td></tr>"
                // mensaje
                + "<tr><td style=\"padding:12px 32px 12px 32px;" + FUENTE + "\">" + cuerpo + "</td></tr>"
                // pie
                + "<tr><td style=\"padding:18px 32px 24px 32px;border-top:1px solid " + LINEA + ";text-align:center;" + FUENTE + "\">"
                + "<div style=\"font-size:13px;font-weight:600;color:" + TEAL + ";\">Vet-Care &middot; Software de gesti&oacute;n veterinaria</div>"
                + "<div style=\"font-size:12px;color:" + GRIS + ";margin-top:4px;\">Este es un mensaje autom&aacute;tico. "
                + "Para cualquier cambio comun&iacute;cate directamente con la cl&iacute;nica.</div>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static boolean esTarjetaDeDatos(String[] lineas) {
        for (String l : lineas) {
            if (!l.startsWith("  ") || l.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String tarjeta(String[] lineas) {
        StringBuilder t = new StringBuilder("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin:0 0 18px 0;background:" + FONDO + ";border-left:4px solid " + VERDE + ";border-radius:6px;\">");
        for (String l : lineas) {
            String linea = l.strip();
            int i = linea.indexOf(": ");
            if (i > 0) {
                t.append("<tr><td style=\"padding:8px 6px 8px 16px;font-size:14px;color:").append(GRIS)
                        .append(";white-space:nowrap;vertical-align:top;\">").append(esc(linea.substring(0, i)))
                        .append("</td><td style=\"padding:8px 16px 8px 6px;font-size:15px;font-weight:600;color:").append(TINTA)
                        .append(";\">").append(esc(capitalizar(linea.substring(i + 2)))).append("</td></tr>");
            } else {
                t.append("<tr><td colspan=\"2\" style=\"padding:8px 16px;font-size:15px;color:").append(TINTA)
                        .append(";\">").append(esc(linea)).append("</td></tr>");
            }
        }
        return t.append("</table>").toString();
    }

    private static String texto(String[] lineas) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < lineas.length; i++) {
            if (i > 0) {
                s.append("<br>");
            }
            s.append(esc(lineas[i].strip()));
        }
        return s.toString();
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
