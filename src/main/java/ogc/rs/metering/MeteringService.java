package ogc.rs.metering;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface MeteringService {
    @GenIgnore
    static MeteringService createProxy(Vertx vertx, String address) {
        return new MeteringServiceVertxEBProxy(vertx, address);
    }

    Future<JsonObject> executeReadQuery(JsonObject request);

    Future<Void> insertIntoPostgresAuditTable(JsonObject request);

    Future<JsonObject> insertMeteringValuesInRmq(JsonObject request);

    Future<JsonObject> monthlyOverview(JsonObject request);

    Future<JsonObject> summaryOverview(JsonObject request);
}
