
DO $$
DECLARE
    new_id UUID;
    crs_id UUID;
BEGIN
    -- Insert collection and get ID
    INSERT INTO collections_details (
        title, description, datetime_key, crs, bbox, temporal, license
    ) VALUES (
        'Catalog of Records',
        'A collection of metadata records describing datasets',
        NULL,
        'http://www.opengis.net/def/crs/OGC/1.3/CRS84',
        '{68.1766,7.9655,97.4025,35.4940}',
        ARRAY[NOW()::TEXT, NOW()::TEXT],
        NULL
    )
    RETURNING id INTO new_id;

    -- Select the crs_id based on srid = 4326
    SELECT id INTO crs_id FROM crs_to_srid WHERE srid = 4326 LIMIT 1;

    INSERT INTO collection_supported_crs (collection_id, crs_id)
    VALUES (new_id, crs_id);

    INSERT INTO collection_type (collection_id, type)
    VALUES (new_id, 'COLLECTION');


    -- Create table with UUID as the table name and SERIAL ID for auto-increment
    EXECUTE format($f$
        CREATE TABLE public.%I (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            title character varying(50) DEFAULT ''::character varying,
            description character varying(100) DEFAULT ''::character varying,
            keywords TEXT[],
            provider_name character varying(100),
            provider_contacts character varying(100),
            bbox NUMERIC[],
            temporal TEXT[],
            geometry GEOMETRY,
            collection_id UUID UNIQUE,
            CONSTRAINT fk_collection
              FOREIGN KEY (collection_id)
              REFERENCES collections_details(id)
              ON DELETE CASCADE
        )
    $f$, new_id);

  EXECUTE format($f$
    GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO %s
$f$, new_id, '${ogcUser}');

END $$ LANGUAGE plpgsql;