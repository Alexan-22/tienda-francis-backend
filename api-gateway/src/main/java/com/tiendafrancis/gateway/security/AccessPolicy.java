package com.tiendafrancis.gateway.security;

import org.springframework.http.HttpMethod;

import java.util.List;

public final class AccessPolicy {

    public enum Nivel {
        PUBLICO,
        AUTENTICADO,
        ADMIN,
        DENEGADO
    }

    private static final List<String> CATALOGO = List.of(
            "/api/productos",
            "/api/categorias",
            "/api/generos",
            "/api/tallas",
            "/api/producto-tallas",
            "/api/producto-imagenes",
            "/api/imagenes"
    );

    private AccessPolicy() {
    }

    public static Nivel resolver(
            HttpMethod metodo,
            String ruta
    ) {
        if (ruta == null || metodo == null) {
            return Nivel.DENEGADO;
        }

        // Salud del Gateway

        if (metodo == HttpMethod.GET
                && ruta.equals("/actuator/health")) {
            return Nivel.PUBLICO;
        }

        // Login y registro

        if (metodo == HttpMethod.POST
                && (
                ruta.equals("/api/auth/login")
                        || ruta.equals("/api/auth/registro")
        )) {
            return Nivel.PUBLICO;
        }

        // Banners públicos

        if (metodo == HttpMethod.GET
                && ruta.equals("/api/home/banners")) {
            return Nivel.PUBLICO;
        }

        // Catálogo: GET público, escrituras ADMIN

        if (CATALOGO.stream()
                .anyMatch(base -> familia(ruta, base))) {

            return metodo == HttpMethod.GET
                    ? Nivel.PUBLICO
                    : escritura(metodo)
                    ? Nivel.ADMIN
                    : Nivel.DENEGADO;
        }

        // Perfil personal

        if (ruta.equals("/api/usuarios/me")
                && (
                metodo == HttpMethod.GET
                        || metodo == HttpMethod.PUT
        )) {
            return Nivel.AUTENTICADO;
        }

        if (ruta.equals("/api/usuarios/me/password")
                && metodo == HttpMethod.PUT) {
            return Nivel.AUTENTICADO;
        }

        // Administración de usuarios

        if (ruta.equals("/api/usuarios")
                && metodo == HttpMethod.GET) {
            return Nivel.ADMIN;
        }

        if (ruta.matches("/api/usuarios/[0-9]+/estado")
                && metodo == HttpMethod.PATCH) {
            return Nivel.ADMIN;
        }

        // Administración de banners

        if (familia(ruta, "/api/contenido/banners")) {

            return metodo == HttpMethod.GET
                    || escritura(metodo)
                    ? Nivel.ADMIN
                    : Nivel.DENEGADO;
        }

        // Reportes, pagos y clientes presenciales

        if (familia(ruta, "/api/reportes")
                || familia(ruta, "/api/pagos")
                || familia(ruta, "/api/clientes-presenciales")) {

            return metodo == HttpMethod.GET
                    || escritura(metodo)
                    ? Nivel.ADMIN
                    : Nivel.DENEGADO;
        }

        // Direcciones del usuario autenticado

        if (familia(ruta, "/api/direcciones")) {

            return metodo == HttpMethod.GET
                    || metodo == HttpMethod.POST
                    || metodo == HttpMethod.DELETE
                    ? Nivel.AUTENTICADO
                    : Nivel.DENEGADO;
        }

        // Compra online

        if (ruta.equals("/api/ventas")
                && metodo == HttpMethod.POST) {
            return Nivel.AUTENTICADO;
        }

        // Historial del cliente

        if (ruta.equals("/api/ventas/mis-pedidos")
                && metodo == HttpMethod.GET) {
            return Nivel.AUTENTICADO;
        }

        // Seguimiento del pedido

        if (ruta.matches("/api/ventas/[0-9]+/seguimiento")
                && metodo == HttpMethod.GET) {
            return Nivel.AUTENTICADO;
        }

        // Punto de venta

        if (ruta.equals("/api/ventas/pos")
                && metodo == HttpMethod.POST) {
            return Nivel.ADMIN;
        }

        // Pagos, cancelaciones y devoluciones

        if (ruta.matches(
                "/api/ventas/[0-9]+/" +
                        "(pagos|cancelar|recepciones|estado|confirmar-pago)"
        )) {
            if (metodo == HttpMethod.POST) {
                return Nivel.ADMIN;
            }

            if (metodo == HttpMethod.GET
                    && (
                    ruta.endsWith("/pagos")
                            || ruta.endsWith("/recepciones")
            )) {
                return Nivel.ADMIN;
            }
        }
        // =====================================================
        // INVENTARIO ADMINISTRATIVO
        // =====================================================

        if (ruta.equals("/api/inventario/reposiciones")
                && metodo == HttpMethod.POST) {

            return Nivel.ADMIN;
        }

        if (ruta.equals("/api/inventario/movimientos")
                && metodo == HttpMethod.GET) {

            return Nivel.ADMIN;
        }

        // Cualquier ruta no definida queda bloqueada

        return Nivel.DENEGADO;
    }

    private static boolean escritura(HttpMethod metodo) {

        return metodo == HttpMethod.POST
                || metodo == HttpMethod.PUT
                || metodo == HttpMethod.PATCH
                || metodo == HttpMethod.DELETE;
    }

    private static boolean familia(
            String ruta,
            String base
    ) {
        return ruta.equals(base)
                || ruta.startsWith(base + "/");
    }
}
