package ogc.rs.apiserver.util;

public class Constants {
    // Header params
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CSV = "csv";
    public static final String HEADER_JSON = "json";
    public static final String HEADER_PARQUET = "parquet";
    public static final String HEADER_HOST = "Host";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ORIGIN = "Origin";
    public static final String HEADER_REFERER = "Referer";
    public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String HEADER_OPTIONS = "options";
    public static final String COUNT_HEADER = "Count";
    public static final String PUBLIC_TOKEN = "public";
    public static final String HEADER_PUBLIC_KEY = "publicKey";
    public static final String HEADER_RESPONSE_FILE_FORMAT = "format";
    public static final String MIME_APPLICATION_JSON = "application/json";
    public static final String LANDING_PAGE = "getLandingPage";
    public static final String CONFORMANCE_CLASSES = "getConformanceClasses";
    public static final String OPENAPI_SPEC = "/api";
    public static final String COLLECTIONS_API = "getCollections";
    public static final String COLLECTION_API = "describeCollection";
    public static final String FEATURES_API = "getFeatures";
    public static final String FEATURE_API = "getFeature";
    public static final String EXECUTE_API = "execute";
    public static final String COLLECTIONS = "collections";
    public static final String TILEMATRIXSETS_API = "getTileMatrixSetsList";
    public static final String TILEMATRIXSET_API = "getTileMatrixSet";
    public static final String TILESETSLIST_API = ".collection.map.getTileSetsList";
    public static final String TILESET_API = ".collection.map.getTileSet";
    public static final String TILE_API = ".collection.map.getTile";

    public static final String STAC = "stac";
    public static final String STAC_OPENAPI_SPEC = "/stac/api";
    public static final String PROCESSES_API = "getProcesses";
    public static final String PROCESS_API = "getProcessDescription";
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String STAC_CATALOG_API = "getStacLandingPage";
    public static final String STAC_COLLECTIONS_API = "getStacCollections";
    public static final String STAC_COLLECTION_API = "describeStacCollection";
    public static final String STAC_ITEMS_API = "getStacItems";
    public static final String STAC_ITEM_BY_ID_API = "getStacItemById";
    public static final String ASSET_API = "getAsset";
    public static final String STAC_CONFORMANCE_CLASSES = "getConformanceDeclaration";
    public static final String PROCESS_EXECUTION_REGEX = "/processes/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/execution";
    public static final String STATUS_API = "getStatus";
    public static final String JOB_STATUS_REGEX = "/jobs/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";
    public static final String METERING_OPENAPI_SPEC = "/metering/api";
    public static final String CONSUMER_AUDIT_API = "consumer/audit";
    public static final String PROVIDER_AUDIT_API = "provider/audit";
    public static final String SUMMARY_AUDIT_API = "summary";
    public static final String OVERVIEW_AUDIT_API = "overview";
    public static final String CAT_SEARCH_PATH = "/search";
    public static final String AUTH_CERTIFICATE_PATH = "/cert";
    public static final String ASSET_NOT_FOUND = "Asset not found";
    public static final String NOT_FOUND = "Not Found";
    public static final String NOT_AUTHORIZED = "Not Authorized";
    public static final String INVALID_COLLECTION_ID = "Invalid collection id";
    public static final String USER_NOT_AUTHORIZED =  "User is not authorised. Please contact IUDX AAA ";
    public static final String NOT_PROVIDER_OR_CONSUMER_TOKEN = "Not a provider or consumer token. It is of role ";
    public static final String RESOURCE_OPEN_TOKEN_SECURE = "Resource is OPEN. Token is SECURE of role ";
    public static final String COVERAGE_SCHEMA ="getCoverageSchema";
    public static final String COLLECTION_COVERAGE = "getCollectionCoverage";
    public static final String COLLECTION_COVERAGE_TYPE = "application/vnd.cov+json";


}
