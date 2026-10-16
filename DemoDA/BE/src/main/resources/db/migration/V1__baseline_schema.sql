--
-- PostgreSQL database dump
--

\restrict OaNj2YNoVzvKG86WSfbqnaueQ5ipcFJxQ4jJeD6R3746Dr4SwnScSZZY7ZgbPkX

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
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
-- Name: batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batches (
    id bigint NOT NULL,
    anchor_status character varying(255),
    batch_code character varying(255) NOT NULL,
    chain_tx_hash character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    distributor_id bigint,
    expiry_date date,
    merkle_root character varying(255),
    name character varying(255) NOT NULL,
    remaining_weight double precision,
    retailer_id bigint,
    status character varying(255) NOT NULL,
    total_weight double precision,
    created_by bigint,
    product_category_id bigint,
    distributor_merkle_root character varying(64),
    producer_merkle_root character varying(64),
    retailer_merkle_root character varying(64),
    transport_route_geojson text,
    retailer_recording_closed boolean DEFAULT false NOT NULL,
    CONSTRAINT batches_anchor_status_check CHECK (((anchor_status)::text = ANY ((ARRAY['NOT_ANCHORED'::character varying, 'ANCHORED'::character varying, 'ANCHOR_FAILED'::character varying])::text[]))),
    CONSTRAINT batches_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'IN_PRODUCTION'::character varying, 'ASSIGNED_TO_DISTRIBUTOR'::character varying, 'RECEIVED_BY_DISTRIBUTOR'::character varying, 'IN_DISTRIBUTION'::character varying, 'DELIVERED_TO_RETAILER'::character varying, 'DELIVERY_REJECTED'::character varying, 'RETURNED_TO_DISTRIBUTOR'::character varying, 'AT_RETAIL'::character varying, 'SOLD_OUT'::character varying])::text[])))
);


--
-- Name: batches_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batches_id_seq OWNED BY public.batches.id;


--
-- Name: evaluation_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluation_results (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    evaluation_model character varying(30) NOT NULL,
    generation_time_ns bigint NOT NULL,
    off_chain_bytes bigint NOT NULL,
    on_chain_bytes bigint NOT NULL,
    proof_generation_time_ns bigint NOT NULL,
    proof_size_bytes integer NOT NULL,
    record_count integer NOT NULL,
    run_id character varying(36) NOT NULL,
    total_gas_used numeric(78,0) NOT NULL,
    transaction_count integer NOT NULL,
    verification_time_ns bigint NOT NULL,
    repetitions integer DEFAULT 1 NOT NULL,
    sample_index integer,
    CONSTRAINT evaluation_results_evaluation_model_check CHECK (((evaluation_model)::text = ANY ((ARRAY['FULL_ON_CHAIN'::character varying, 'HASH_PER_RECORD'::character varying, 'MERKLE_BATCHING'::character varying])::text[])))
);


--
-- Name: evaluation_results_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evaluation_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: evaluation_results_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evaluation_results_id_seq OWNED BY public.evaluation_results.id;


--
-- Name: producer_partners; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.producer_partners (
    id bigint NOT NULL,
    active boolean NOT NULL,
    partner_id bigint NOT NULL,
    producer_id bigint NOT NULL
);


--
-- Name: producer_partners_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.producer_partners_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: producer_partners_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.producer_partners_id_seq OWNED BY public.producer_partners.id;


--
-- Name: product_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_categories (
    id bigint NOT NULL,
    active boolean,
    description character varying(255),
    dried_product boolean,
    humidity_max double precision,
    humidity_min double precision,
    name character varying(255) NOT NULL,
    recommended_packaging character varying(255),
    temperature_max double precision,
    temperature_min double precision
);


--
-- Name: product_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_categories_id_seq OWNED BY public.product_categories.id;


--
-- Name: records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.records (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    leaf_hash character varying(255),
    leaf_index integer,
    raw_json text NOT NULL,
    record_key character varying(255) NOT NULL,
    record_type smallint NOT NULL,
    batch_id bigint NOT NULL,
    ipfs_cid character varying(100),
    record_stage character varying(255),
    stage_leaf_index integer,
    hash_version integer,
    leaf_salt character varying(64),
    encrypted boolean,
    encryption_version integer,
    CONSTRAINT records_record_stage_check CHECK (((record_stage)::text = ANY ((ARRAY['PRODUCER'::character varying, 'DISTRIBUTOR'::character varying, 'RETAILER'::character varying])::text[]))),
    CONSTRAINT records_record_type_check CHECK ((record_type = ANY (ARRAY[0, 1, 2, 3, 4, 5, 6, 7])))
);


--
-- Name: records_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.records_id_seq OWNED BY public.records.id;


--
-- Name: retail_units; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.retail_units (
    id bigint NOT NULL,
    allocated_weight double precision NOT NULL,
    created_at timestamp(6) without time zone,
    expiry_date date,
    package_quantity integer,
    package_weight double precision,
    price_per_kg double precision,
    price_per_package double precision,
    product_name character varying(255) NOT NULL,
    qr_content character varying(255),
    retail_code character varying(255) NOT NULL,
    retailer_id bigint NOT NULL,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    batch_id bigint NOT NULL,
    retail_record_key character varying(100),
    CONSTRAINT retail_units_status_check CHECK (((status)::text = ANY ((ARRAY['IN_STOCK'::character varying, 'SOLD_OUT'::character varying])::text[]))),
    CONSTRAINT retail_units_type_check CHECK (((type)::text = ANY ((ARRAY['BULK'::character varying, 'PACKAGED'::character varying])::text[])))
);


--
-- Name: retail_units_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.retail_units_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: retail_units_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.retail_units_id_seq OWNED BY public.retail_units.id;


--
-- Name: stage_anchor_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stage_anchor_transactions (
    id bigint NOT NULL,
    anchored_at timestamp(6) without time zone NOT NULL,
    block_number numeric(78,0) NOT NULL,
    gas_used numeric(78,0) NOT NULL,
    record_stage character varying(30) NOT NULL,
    transaction_hash character varying(100) NOT NULL,
    batch_id bigint NOT NULL,
    CONSTRAINT stage_anchor_transactions_record_stage_check CHECK (((record_stage)::text = ANY ((ARRAY['PRODUCER'::character varying, 'DISTRIBUTOR'::character varying, 'RETAILER'::character varying])::text[])))
);


--
-- Name: stage_anchor_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stage_anchor_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stage_anchor_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stage_anchor_transactions_id_seq OWNED BY public.stage_anchor_transactions.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255),
    enabled boolean NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    username character varying(255) NOT NULL,
    company_address character varying(500),
    company_name character varying(200),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'PRODUCER'::character varying, 'DISTRIBUTOR'::character varying, 'RETAILER'::character varying, 'CONSUMER'::character varying])::text[])))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vehicles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehicles (
    id bigint NOT NULL,
    active boolean,
    device_id character varying(255),
    distributor_id bigint NOT NULL,
    sensor_firmware character varying(255),
    vehicle_plate character varying(255),
    device_public_key character varying(200),
    last_sequence bigint
);


--
-- Name: vehicles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vehicles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vehicles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vehicles_id_seq OWNED BY public.vehicles.id;


--
-- Name: batches id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches ALTER COLUMN id SET DEFAULT nextval('public.batches_id_seq'::regclass);


--
-- Name: evaluation_results id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_results ALTER COLUMN id SET DEFAULT nextval('public.evaluation_results_id_seq'::regclass);


--
-- Name: producer_partners id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producer_partners ALTER COLUMN id SET DEFAULT nextval('public.producer_partners_id_seq'::regclass);


--
-- Name: product_categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_categories ALTER COLUMN id SET DEFAULT nextval('public.product_categories_id_seq'::regclass);


--
-- Name: records id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.records ALTER COLUMN id SET DEFAULT nextval('public.records_id_seq'::regclass);


--
-- Name: retail_units id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.retail_units ALTER COLUMN id SET DEFAULT nextval('public.retail_units_id_seq'::regclass);


--
-- Name: stage_anchor_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stage_anchor_transactions ALTER COLUMN id SET DEFAULT nextval('public.stage_anchor_transactions_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: vehicles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicles ALTER COLUMN id SET DEFAULT nextval('public.vehicles_id_seq'::regclass);


--
-- Name: batches batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT batches_pkey PRIMARY KEY (id);


--
-- Name: evaluation_results evaluation_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_results
    ADD CONSTRAINT evaluation_results_pkey PRIMARY KEY (id);


--
-- Name: producer_partners producer_partners_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producer_partners
    ADD CONSTRAINT producer_partners_pkey PRIMARY KEY (id);


--
-- Name: product_categories product_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT product_categories_pkey PRIMARY KEY (id);


--
-- Name: records records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.records
    ADD CONSTRAINT records_pkey PRIMARY KEY (id);


--
-- Name: retail_units retail_units_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.retail_units
    ADD CONSTRAINT retail_units_pkey PRIMARY KEY (id);


--
-- Name: stage_anchor_transactions stage_anchor_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stage_anchor_transactions
    ADD CONSTRAINT stage_anchor_transactions_pkey PRIMARY KEY (id);


--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: stage_anchor_transactions uk_batch_record_stage; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stage_anchor_transactions
    ADD CONSTRAINT uk_batch_record_stage UNIQUE (batch_id, record_stage);


--
-- Name: stage_anchor_transactions uk_i72uvkpl3055h85w1tygnota1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stage_anchor_transactions
    ADD CONSTRAINT uk_i72uvkpl3055h85w1tygnota1 UNIQUE (transaction_hash);


--
-- Name: batches uk_i9rhmcfbg6x29l417eep0u3f5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT uk_i9rhmcfbg6x29l417eep0u3f5 UNIQUE (batch_code);


--
-- Name: retail_units uk_nlsqln19udnnxmn1mfxe857t8; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.retail_units
    ADD CONSTRAINT uk_nlsqln19udnnxmn1mfxe857t8 UNIQUE (retail_code);


--
-- Name: producer_partners uk_producer_partner; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producer_partners
    ADD CONSTRAINT uk_producer_partner UNIQUE (producer_id, partner_id);


--
-- Name: users uk_r43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_r43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vehicles vehicles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT vehicles_pkey PRIMARY KEY (id);


--
-- Name: batches fk6mo9j1wnhavvk29if4bufi6gb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT fk6mo9j1wnhavvk29if4bufi6gb FOREIGN KEY (product_category_id) REFERENCES public.product_categories(id);


--
-- Name: records fke2lw3po5fa2tqqol3fdvxuds4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.records
    ADD CONSTRAINT fke2lw3po5fa2tqqol3fdvxuds4 FOREIGN KEY (batch_id) REFERENCES public.batches(id);


--
-- Name: producer_partners fkg8dwqwbafxlcblo01yvbagkr9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producer_partners
    ADD CONSTRAINT fkg8dwqwbafxlcblo01yvbagkr9 FOREIGN KEY (producer_id) REFERENCES public.users(id);


--
-- Name: stage_anchor_transactions fkhkg9g0oibco1maerk07m4pln3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stage_anchor_transactions
    ADD CONSTRAINT fkhkg9g0oibco1maerk07m4pln3 FOREIGN KEY (batch_id) REFERENCES public.batches(id);


--
-- Name: batches fkix3ux2vfrgpr80vvd5cqdg6il; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT fkix3ux2vfrgpr80vvd5cqdg6il FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: retail_units fkjkt6lxcfm8666uac3s03otmvn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.retail_units
    ADD CONSTRAINT fkjkt6lxcfm8666uac3s03otmvn FOREIGN KEY (batch_id) REFERENCES public.batches(id);


--
-- Name: producer_partners fkm2gccf8xtgh7i722gd1fltn7t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.producer_partners
    ADD CONSTRAINT fkm2gccf8xtgh7i722gd1fltn7t FOREIGN KEY (partner_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict OaNj2YNoVzvKG86WSfbqnaueQ5ipcFJxQ4jJeD6R3746Dr4SwnScSZZY7ZgbPkX

