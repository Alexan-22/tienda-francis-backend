--
-- PostgreSQL database dump
--

\restrict xl66WIQQgDIIWk6O9sBlOYEFQyA9Xzrafs6RsneobcCBetzYx7Yt2rqcR075okg

-- Dumped from database version 17.11
-- Dumped by pg_dump version 17.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: detalle_ventas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.detalle_ventas (
    id bigint NOT NULL,
    venta_id bigint NOT NULL,
    variante_id bigint NOT NULL,
    producto_nombre character varying(255) NOT NULL,
    talla character varying(30) NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    CONSTRAINT detalle_ventas_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT detalle_ventas_precio_unitario_check CHECK ((precio_unitario > (0)::numeric))
);


--
-- Name: detalle_ventas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.detalle_ventas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detalle_ventas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detalle_ventas_id_seq OWNED BY public.detalle_ventas.id;


--
-- Name: devoluciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.devoluciones (
    id bigint NOT NULL,
    pago_id bigint NOT NULL,
    idempotencia uuid NOT NULL,
    monto numeric(12,2) NOT NULL,
    motivo character varying(500) NOT NULL,
    responsable_id bigint NOT NULL,
    creada timestamp with time zone NOT NULL,
    CONSTRAINT devoluciones_monto_check CHECK ((monto > (0)::numeric))
);


--
-- Name: devoluciones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.devoluciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: devoluciones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.devoluciones_id_seq OWNED BY public.devoluciones.id;


--
-- Name: direcciones_envio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.direcciones_envio (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    destinatario character varying(120) NOT NULL,
    linea character varying(255) NOT NULL,
    distrito character varying(100) NOT NULL,
    provincia character varying(100) NOT NULL,
    departamento character varying(100) NOT NULL,
    activa boolean DEFAULT true NOT NULL
);


--
-- Name: direcciones_envio_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.direcciones_envio_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: direcciones_envio_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.direcciones_envio_id_seq OWNED BY public.direcciones_envio.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: historial_pedidos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.historial_pedidos (
    id bigint NOT NULL,
    venta_id bigint NOT NULL,
    origen character varying(30) NOT NULL,
    destino character varying(30) NOT NULL,
    responsable_id bigint,
    motivo character varying(500) NOT NULL,
    ocurrido_en timestamp with time zone NOT NULL
);


--
-- Name: historial_pedidos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.historial_pedidos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: historial_pedidos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.historial_pedidos_id_seq OWNED BY public.historial_pedidos.id;


--
-- Name: movimientos_inventario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.movimientos_inventario (
    id bigint NOT NULL,
    variante_id bigint NOT NULL,
    recepcion_id bigint NOT NULL,
    cantidad integer NOT NULL,
    tipo character varying(30) NOT NULL,
    motivo character varying(500) NOT NULL,
    responsable_id bigint NOT NULL,
    creado_en timestamp with time zone NOT NULL,
    CONSTRAINT movimientos_inventario_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT movimientos_inventario_tipo_check CHECK (((tipo)::text = 'REPOSICION_DEVOLUCION'::text))
);


--
-- Name: movimientos_inventario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.movimientos_inventario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: movimientos_inventario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.movimientos_inventario_id_seq OWNED BY public.movimientos_inventario.id;


--
-- Name: pagos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pagos (
    id bigint NOT NULL,
    venta_id bigint NOT NULL,
    idempotencia uuid NOT NULL,
    monto numeric(12,2) NOT NULL,
    medio character varying(20) NOT NULL,
    referencia character varying(255),
    responsable_id bigint NOT NULL,
    estado character varying(30) DEFAULT 'CONFIRMADO'::character varying NOT NULL,
    creado timestamp with time zone NOT NULL,
    anulado_en timestamp with time zone,
    motivo_anulacion character varying(500),
    CONSTRAINT pagos_check CHECK (((((estado)::text = 'ANULADO'::text) AND (anulado_en IS NOT NULL) AND (motivo_anulacion IS NOT NULL)) OR (((estado)::text = 'CONFIRMADO'::text) AND (anulado_en IS NULL) AND (motivo_anulacion IS NULL)))),
    CONSTRAINT pagos_estado_check CHECK (((estado)::text = ANY ((ARRAY['CONFIRMADO'::character varying, 'ANULADO'::character varying])::text[]))),
    CONSTRAINT pagos_medio_check CHECK (((medio)::text = ANY ((ARRAY['EFECTIVO'::character varying, 'YAPE'::character varying, 'TARJETA'::character varying])::text[]))),
    CONSTRAINT pagos_monto_check CHECK ((monto > (0)::numeric))
);


--
-- Name: pagos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pagos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pagos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pagos_id_seq OWNED BY public.pagos.id;


--
-- Name: producto_tallas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.producto_tallas (
    id bigint NOT NULL,
    producto_id bigint NOT NULL,
    talla character varying(30) NOT NULL,
    stock_fisico integer DEFAULT 0 NOT NULL,
    stock_reservado integer DEFAULT 0 NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT producto_tallas_check CHECK (((stock_reservado >= 0) AND (stock_reservado <= stock_fisico))),
    CONSTRAINT producto_tallas_stock_fisico_check CHECK ((stock_fisico >= 0))
);


--
-- Name: producto_tallas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.producto_tallas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: producto_tallas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.producto_tallas_id_seq OWNED BY public.producto_tallas.id;


--
-- Name: productos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.productos (
    id bigint NOT NULL,
    nombre character varying(255) NOT NULL,
    precio numeric(12,2) NOT NULL,
    precio_oferta numeric(12,2),
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT productos_check CHECK (((precio_oferta > (0)::numeric) AND (precio_oferta < precio))),
    CONSTRAINT productos_precio_check CHECK ((precio > (0)::numeric))
);


--
-- Name: productos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.productos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: productos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.productos_id_seq OWNED BY public.productos.id;


--
-- Name: recepciones_devolucion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recepciones_devolucion (
    id bigint NOT NULL,
    venta_id bigint NOT NULL,
    detalle_id bigint NOT NULL,
    idempotencia uuid NOT NULL,
    cantidad integer NOT NULL,
    condicion character varying(20) NOT NULL,
    motivo character varying(500) NOT NULL,
    responsable_id bigint NOT NULL,
    repuesto boolean NOT NULL,
    recibido_en timestamp with time zone NOT NULL,
    CONSTRAINT recepciones_devolucion_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT recepciones_devolucion_check CHECK ((repuesto = ((condicion)::text = 'APTO'::text))),
    CONSTRAINT recepciones_devolucion_condicion_check CHECK (((condicion)::text = ANY ((ARRAY['APTO'::character varying, 'DANADO'::character varying])::text[]))),
    CONSTRAINT recepciones_devolucion_motivo_check CHECK ((length(TRIM(BOTH FROM motivo)) > 0))
);


--
-- Name: recepciones_devolucion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recepciones_devolucion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recepciones_devolucion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recepciones_devolucion_id_seq OWNED BY public.recepciones_devolucion.id;


--
-- Name: ventas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ventas (
    id bigint NOT NULL,
    idempotencia uuid NOT NULL,
    usuario_id bigint,
    tipo character varying(20) NOT NULL,
    estado character varying(30) NOT NULL,
    pago_estado character varying(30) NOT NULL,
    total numeric(12,2) NOT NULL,
    creada timestamp with time zone NOT NULL,
    vence timestamp with time zone,
    responsable_id bigint,
    cliente_presencial_nombre character varying(255),
    solicitud_hash character varying(64),
    direccion_id bigint,
    direccion_snapshot text,
    subtotal numeric(12,2),
    delivery_costo numeric(12,2),
    CONSTRAINT ck_delivery_costo CHECK (((delivery_costo IS NULL) OR (delivery_costo >= (0)::numeric))),
    CONSTRAINT ck_tipo CHECK (((tipo)::text = ANY ((ARRAY['ONLINE'::character varying, 'PRESENCIAL'::character varying])::text[]))),
    CONSTRAINT ck_venta_total CHECK ((total >= (0)::numeric)),
    CONSTRAINT ventas_total_check CHECK ((total >= (0)::numeric))
);


--
-- Name: ventas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ventas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ventas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ventas_id_seq OWNED BY public.ventas.id;


--
-- Name: detalle_ventas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_ventas ALTER COLUMN id SET DEFAULT nextval('public.detalle_ventas_id_seq'::regclass);


--
-- Name: devoluciones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devoluciones ALTER COLUMN id SET DEFAULT nextval('public.devoluciones_id_seq'::regclass);


--
-- Name: direcciones_envio id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.direcciones_envio ALTER COLUMN id SET DEFAULT nextval('public.direcciones_envio_id_seq'::regclass);


--
-- Name: historial_pedidos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_pedidos ALTER COLUMN id SET DEFAULT nextval('public.historial_pedidos_id_seq'::regclass);


--
-- Name: movimientos_inventario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario ALTER COLUMN id SET DEFAULT nextval('public.movimientos_inventario_id_seq'::regclass);


--
-- Name: pagos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos ALTER COLUMN id SET DEFAULT nextval('public.pagos_id_seq'::regclass);


--
-- Name: producto_tallas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producto_tallas ALTER COLUMN id SET DEFAULT nextval('public.producto_tallas_id_seq'::regclass);


--
-- Name: productos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos ALTER COLUMN id SET DEFAULT nextval('public.productos_id_seq'::regclass);


--
-- Name: recepciones_devolucion id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recepciones_devolucion ALTER COLUMN id SET DEFAULT nextval('public.recepciones_devolucion_id_seq'::regclass);


--
-- Name: ventas id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas ALTER COLUMN id SET DEFAULT nextval('public.ventas_id_seq'::regclass);


--
-- Name: detalle_ventas detalle_ventas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_ventas
    ADD CONSTRAINT detalle_ventas_pkey PRIMARY KEY (id);


--
-- Name: devoluciones devoluciones_idempotencia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devoluciones
    ADD CONSTRAINT devoluciones_idempotencia_key UNIQUE (idempotencia);


--
-- Name: devoluciones devoluciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devoluciones
    ADD CONSTRAINT devoluciones_pkey PRIMARY KEY (id);


--
-- Name: direcciones_envio direcciones_envio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.direcciones_envio
    ADD CONSTRAINT direcciones_envio_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: historial_pedidos historial_pedidos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_pedidos
    ADD CONSTRAINT historial_pedidos_pkey PRIMARY KEY (id);


--
-- Name: movimientos_inventario movimientos_inventario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_pkey PRIMARY KEY (id);


--
-- Name: movimientos_inventario movimientos_inventario_recepcion_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_recepcion_id_key UNIQUE (recepcion_id);


--
-- Name: pagos pagos_idempotencia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_idempotencia_key UNIQUE (idempotencia);


--
-- Name: pagos pagos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_pkey PRIMARY KEY (id);


--
-- Name: producto_tallas producto_tallas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producto_tallas
    ADD CONSTRAINT producto_tallas_pkey PRIMARY KEY (id);


--
-- Name: producto_tallas producto_tallas_producto_id_talla_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producto_tallas
    ADD CONSTRAINT producto_tallas_producto_id_talla_key UNIQUE (producto_id, talla);


--
-- Name: productos productos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.productos
    ADD CONSTRAINT productos_pkey PRIMARY KEY (id);


--
-- Name: recepciones_devolucion recepciones_devolucion_idempotencia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recepciones_devolucion
    ADD CONSTRAINT recepciones_devolucion_idempotencia_key UNIQUE (idempotencia);


--
-- Name: recepciones_devolucion recepciones_devolucion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recepciones_devolucion
    ADD CONSTRAINT recepciones_devolucion_pkey PRIMARY KEY (id);


--
-- Name: ventas ventas_idempotencia_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas
    ADD CONSTRAINT ventas_idempotencia_key UNIQUE (idempotencia);


--
-- Name: ventas ventas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas
    ADD CONSTRAINT ventas_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: ix_devoluciones_pago; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_devoluciones_pago ON public.devoluciones USING btree (pago_id);


--
-- Name: ix_direcciones_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_direcciones_usuario ON public.direcciones_envio USING btree (usuario_id, activa);


--
-- Name: ix_historial_pedido_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_historial_pedido_fecha ON public.historial_pedidos USING btree (venta_id, ocurrido_en, id);


--
-- Name: ix_movimientos_variante_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_movimientos_variante_fecha ON public.movimientos_inventario USING btree (variante_id, creado_en);


--
-- Name: ix_pagos_venta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pagos_venta ON public.pagos USING btree (venta_id);


--
-- Name: ix_recepciones_detalle; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_recepciones_detalle ON public.recepciones_devolucion USING btree (detalle_id);


--
-- Name: ix_recepciones_venta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_recepciones_venta ON public.recepciones_devolucion USING btree (venta_id);


--
-- Name: ix_ventas_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ventas_usuario ON public.ventas USING btree (usuario_id, creada);


--
-- Name: ix_ventas_vence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ventas_vence ON public.ventas USING btree (vence, id) WHERE (((estado)::text = 'RECIBIDO'::text) AND ((pago_estado)::text = 'PENDIENTE'::text) AND ((tipo)::text = 'ONLINE'::text));


--
-- Name: detalle_ventas detalle_ventas_variante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_ventas
    ADD CONSTRAINT detalle_ventas_variante_id_fkey FOREIGN KEY (variante_id) REFERENCES public.producto_tallas(id);


--
-- Name: detalle_ventas detalle_ventas_venta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_ventas
    ADD CONSTRAINT detalle_ventas_venta_id_fkey FOREIGN KEY (venta_id) REFERENCES public.ventas(id);


--
-- Name: devoluciones devoluciones_pago_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devoluciones
    ADD CONSTRAINT devoluciones_pago_id_fkey FOREIGN KEY (pago_id) REFERENCES public.pagos(id);


--
-- Name: historial_pedidos historial_pedidos_venta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_pedidos
    ADD CONSTRAINT historial_pedidos_venta_id_fkey FOREIGN KEY (venta_id) REFERENCES public.ventas(id);


--
-- Name: movimientos_inventario movimientos_inventario_recepcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_recepcion_id_fkey FOREIGN KEY (recepcion_id) REFERENCES public.recepciones_devolucion(id);


--
-- Name: movimientos_inventario movimientos_inventario_variante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_inventario
    ADD CONSTRAINT movimientos_inventario_variante_id_fkey FOREIGN KEY (variante_id) REFERENCES public.producto_tallas(id);


--
-- Name: pagos pagos_venta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pagos
    ADD CONSTRAINT pagos_venta_id_fkey FOREIGN KEY (venta_id) REFERENCES public.ventas(id);


--
-- Name: producto_tallas producto_tallas_producto_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producto_tallas
    ADD CONSTRAINT producto_tallas_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES public.productos(id);


--
-- Name: recepciones_devolucion recepciones_devolucion_detalle_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recepciones_devolucion
    ADD CONSTRAINT recepciones_devolucion_detalle_id_fkey FOREIGN KEY (detalle_id) REFERENCES public.detalle_ventas(id);


--
-- Name: recepciones_devolucion recepciones_devolucion_venta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recepciones_devolucion
    ADD CONSTRAINT recepciones_devolucion_venta_id_fkey FOREIGN KEY (venta_id) REFERENCES public.ventas(id);


--
-- Name: ventas ventas_direccion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ventas
    ADD CONSTRAINT ventas_direccion_id_fkey FOREIGN KEY (direccion_id) REFERENCES public.direcciones_envio(id);


--
-- PostgreSQL database dump complete
--

\unrestrict xl66WIQQgDIIWk6O9sBlOYEFQyA9Xzrafs6RsneobcCBetzYx7Yt2rqcR075okg

