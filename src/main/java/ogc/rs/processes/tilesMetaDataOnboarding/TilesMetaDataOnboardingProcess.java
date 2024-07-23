package ogc.rs.processes.tilesMetaDataOnboarding;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;

import static ogc.rs.processes.tilesMetaDataOnboarding.Constants.*;

import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;

/**
 * Handles the onboarding process for tiles, including file existence checks,
 * resource ownership verification, collection type validation, and collection
 * existence checks.
 */

public class TilesMetaDataOnboardingProcess implements ProcessService {
    private static final Logger LOGGER = LogManager.getLogger(TilesMetaDataOnboardingProcess.class);
    private final Vertx vertx;
    private final PgPool pgPool;
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private final DataFromS3 dataFromS3;
    private String databaseName;
    private String databaseHost;
    private String databasePort;
    private String databaseUser;
    private String databasePassword;

    /**
     * Constructs a TilesMetaDataOnboardingProcess.
     *
     * @param pgPool       PostgreSQL database connection pool
     * @param webClient    Vert.x web client for making HTTP requests
     * @param config       Configuration JSON object containing database and other settings
     * @param dataFromS3   DataFromS3 instance for interacting with AWS S3
     * @param vertx        Vert.x instance for asynchronous event-driven programming
     */
    public TilesMetaDataOnboardingProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx){
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        this.dataFromS3=dataFromS3;
        this.vertx = vertx;
        initializeConfig(config);
    }

    /**
     * Initializes the database configuration parameters.
     *
     * @param config Configuration JSON object containing database settings
     */
    private void initializeConfig(JsonObject config){
        this.databaseName = config.getString("databaseName");
        this.databaseHost = config.getString("databaseHost");
        this.databasePort = config.getString("databasePort");
        this.databaseUser = config.getString("databaseUser");
        this.databasePassword = config.getString("databasePassword");
    }

    /**
     * Executes the tiles onboarding process asynchronously.
     *
     * @param requestInput Input JSON object containing collection and tile matrix set details
     * @return Future<JsonObject> a Future containing the result JSON object after completion
     */
    public Future<JsonObject> execute(JsonObject requestInput){
        Promise<JsonObject> promise = Promise.promise();
        String collectionId = requestInput.getString("collectionId");
        String tileMatrixSet = requestInput.getString("tileMatrixSet");
        String fileName = collectionId + "/" + tileMatrixSet + "/";
        requestInput.put("fileName",fileName);
        requestInput.put("progress",calculateProgress(1,7));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, START_TILES_ONBOARDING_PROCESS)
                .compose(progressUpdateHandler -> checkFileExistenceInS3(requestInput))
                .compose(s3FileExistenceHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2, 7)).put("message", S3_FILE_EXISTENCE_MESSAGE)))
                .compose(progressUpdateHandler -> collectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3, 7)).put("message", RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkCollectionType(requestInput))
                .compose(checkCollectionTypeHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4, 7)).put("message", COLLECTION_TYPE_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkCollectionExistence(requestInput))
                .compose(checkCollectionExistenceHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(5, 7)).put("message", COLLECTION_EXISTENCE_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> checkTileMatrixSet(requestInput))
                .compose(tileMatrixCheckHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(6, 7)).put("message", TILE_MATRIX_SET_FOUND_MESSAGE)))
                .compose(progressUpdateHandler -> onboardTileMetadata(requestInput))
                .compose(tilesMetaDataOnboardingHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL,PROCESS_SUCCESS_MESSAGE))
                .onSuccess(successHandler -> {
                    LOGGER.debug(TILES_ONBOARDING_SUCCESS_MESSAGE);
                    promise.complete();
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(TILES_ONBOARDING_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler.getMessage(), promise);
                });
        return promise.future();
    }

    /**
     * Checks if a specific file exists in the AWS S3 bucket.
     *
     * @param requestInput Input JSON object containing file details
     * @return Future<Boolean> a Future containing a boolean indicating file existence in S3
     */
    public Future<Boolean> checkFileExistenceInS3(JsonObject requestInput) {
        Promise<Boolean> promise = Promise.promise();
        String fileName = requestInput.getString("fileName");
        String urlString = dataFromS3.getFullyQualifiedUrlString(fileName);
        dataFromS3.setUrlFromString(urlString);
        dataFromS3.setSignatureHeader(HttpMethod.HEAD);
        dataFromS3.getDataFromS3(HttpMethod.HEAD)
                .onSuccess(responseFromS3 -> {
                    BigInteger fileSize = new BigInteger(responseFromS3.getHeader("Content-Length"));
                    if (fileSize.compareTo(BigInteger.ZERO) > 0) {
                        LOGGER.debug(S3_FILE_EXISTENCE_MESSAGE + " with size: {}", fileSize);
                        promise.complete(true);
                    } else {
                        promise.fail(S3_FILE_EXISTENCE_FAIL_MESSAGE);
                    }
                })
                .onFailure(failed -> {
                    LOGGER.error("Failed to get response from S3: " + failed.getLocalizedMessage());
                    promise.fail(failed.getMessage());
                });

        return promise.future();
    }

    /**
     * Checks the type of the collection to determine suitability for tile metadata onboarding.
     *
     * @param requestBody Input JSON object containing collection details
     * @return Future<Void> a Future indicating completion of the collection type check
     */
    private Future<Void> checkCollectionType(JsonObject requestBody){
        Promise<Void> promise = Promise.promise();
        String collectionType = requestBody.getString("collectionType");
        if ("feature".equalsIgnoreCase(collectionType)) {
            LOGGER.debug(FEATURE_COLLECTION_MESSAGE);
            promise.fail(FEATURE_COLLECTION_MESSAGE);
        }
        return promise.future();
    }

    /**
     * Checks the existence of the collection and determines if it is a pure tile collection
     * or a combination of feature and tile collection.
     *
     * @param requestInput Input JSON object containing collection details
     * @return Future<JsonObject> a Future containing the updated JSON object with the pureTile attribute
     */
    private Future<JsonObject> checkCollectionExistence(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        String collectionId = requestInput.getString("collectionId");

        // Check if the collection exists in the collection_details table
        pgPool.preparedQuery(CHECK_COLLECTION_EXISTENCE_QUERY)
                .execute(Tuple.of(collectionId))
                .compose(rowSet -> {
                    if (rowSet.iterator().hasNext() && rowSet.iterator().next().getBoolean("exists")) {
                        // Collection exists, now check its type in the collection_type table
                        return pgPool.preparedQuery(GET_COLLECTION_TYPE_QUERY)
                                .execute(Tuple.of(collectionId));
                    } else {
                        // Collection does not exist, it is a pure tile collection
                        requestInput.put("pureTile", true);
                        promise.complete(requestInput);
                        return Future.succeededFuture();
                    }
                })
                .onSuccess(typeRowSet -> {
                    if (typeRowSet != null && typeRowSet.iterator().hasNext()) {
                        String collectionType = typeRowSet.iterator().next().getString("type");
                        if ("feature".equalsIgnoreCase(collectionType)) {
                            requestInput.put("pureTile", false);
                            promise.complete(requestInput);
                        } else if ("map".equalsIgnoreCase(collectionType) || "vector".equalsIgnoreCase(collectionType)) {
                            promise.fail(COLLECTION_EXISTS_MESSAGE);
                        } else {
                            promise.fail(UNKNOWN_COLLECTION_TYPE);
                        }
                    } else {
                        promise.fail(COLLECTION_TYPE_NOT_FOUND_MESSAGE);
                    }
                })
                .onFailure(err -> {
                    promise.fail(COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE + " :" + err.getMessage());
                });

        return promise.future();
    }

    /**
     * Checks if the tileMatrixSet exists in the tms_metadata table and adds 'id' and 'crs'
     * column values into the requestInput JSON object.
     *
     * @param requestInput Input JSON object containing tile matrix set details
     * @return Future<JsonObject> a Future containing the updated JSON object with 'id' and 'crs' values if the tile matrix set exists
     */
    private Future<JsonObject> checkTileMatrixSet(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();
        String tmsTitle = requestInput.getString("tileMatrixSet");

        pgPool.preparedQuery(CHECK_TILE_MATRIX_SET_EXISTENCE_QUERY)
                .execute(Tuple.of(tmsTitle))
                .onSuccess(rowSet -> {
                    if (rowSet.iterator().hasNext()) {
                        var row = rowSet.iterator().next();
                        if (row.getBoolean("exists")) {
                            LOGGER.debug(TILE_MATRIX_SET_FOUND_MESSAGE + ": " + tmsTitle);
                            // Add 'id' and 'crs' column values into requestInput
                            requestInput.put("tms_id", row.getValue("id"));
                            requestInput.put("crs", row.getValue("crs"));
                            promise.complete(requestInput);
                        } else {
                            LOGGER.error(TILE_MATRIX_SET_NOT_FOUND_MESSAGE + ": " + tmsTitle);
                            promise.fail(TILE_MATRIX_SET_NOT_FOUND_MESSAGE);
                        }
                    } else {
                        LOGGER.error(TILE_MATRIX_SET_NOT_FOUND_MESSAGE + ": " + tmsTitle);
                        promise.fail(TILE_MATRIX_SET_NOT_FOUND_MESSAGE);
                    }
                })
                .onFailure(err -> {
                    LOGGER.error(TILE_MATRIX_SET_CHECK_FAILURE_MESSAGE + ": " + err.getMessage());
                    promise.fail(TILE_MATRIX_SET_CHECK_FAILURE_MESSAGE + ": " + err.getMessage());
                });

        return promise.future();
    }

    /**
     * Onboards tile metadata into the database. Depending on whether the collection is a pure tile collection,
     * it inserts data into different tables.
     *
     * @param requestInput JSON object containing tile metadata information and other details.
     * @return Future<Void> indicating the success or failure of the onboarding process.
     */
    private Future<Void> onboardTileMetadata(JsonObject requestInput) {
        return pgPool.withTransaction(sqlClient -> {
            Promise<Void> promise = Promise.promise();

            boolean pureTile = requestInput.getBoolean("pureTile");
            String collectionId = requestInput.getString("collectionId");
            String title = requestInput.getString("title");
            String description = requestInput.getString("description");
            String crs = requestInput.getString("crs");
            JsonArray bbox = requestInput.getJsonArray("bbox");
            JsonArray temporal = requestInput.getJsonArray("temporal");
            String accessPolicy = requestInput.getString("accessPolicy");
            String userId = requestInput.getString("userId");
            String collectionType = requestInput.getString("collectionType");
            String tmsId = requestInput.getString("tms_id");
            JsonArray pointOfOrigin = requestInput.getJsonArray("pointOfOrigin");

            Future<Void> collectionDetailsFuture = Future.succeededFuture();
            if (pureTile) {
                collectionDetailsFuture = sqlClient.preparedQuery(INSERT_COLLECTION_DETAILS_QUERY)
                        .execute(Tuple.of(collectionId, title, description, crs, bbox.encode(), temporal.encode()))
                        .mapEmpty()
                        .compose(v -> sqlClient.preparedQuery(INSERT_RI_DETAILS_QUERY)
                                .execute(Tuple.of(collectionId, accessPolicy, userId))
                                .mapEmpty());
            }

            return collectionDetailsFuture
                    .compose(v -> sqlClient.preparedQuery(INSERT_COLLECTION_TYPE_QUERY)
                            .execute(Tuple.of(collectionId, collectionType))
                            .mapEmpty())
                    .compose(v -> sqlClient.preparedQuery(INSERT_TILE_MATRIX_SET_RELATION_QUERY)
                            .execute(Tuple.of(collectionId, tmsId, pointOfOrigin.encode()))
                            .mapEmpty())
                    .onSuccess(v -> promise.complete())
                    .onFailure(promise::fail)
                    .mapEmpty();
        });
    }

    /**
     * Handles failure scenarios by updating the job table status and failing the promise.
     *
     * @param requestInput Input JSON object containing request details
     * @param errorMessage Error message describing the failure reason
     * @param promise      Promise to fail with the error message
     */
    private void handleFailure(JsonObject requestInput, String errorMessage, Promise<JsonObject> promise) {

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Process failed: {}", errorMessage);
                    promise.fail(errorMessage);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(HANDLE_FAILURE_MESSAGE + ": " + failureHandler.getMessage());
                    promise.fail(HANDLE_FAILURE_MESSAGE);
                });

    }

    /**
     * Calculates the progress percentage based on the current step and total steps.
     *
     * @param currentStep Current step number in the process
     * @param totalSteps  Total number of steps in the process
     * @return Progress percentage as a float value
     */
    private float calculateProgress(int currentStep, int totalSteps){
        return ((float) currentStep / totalSteps) * 100;

    }
}
