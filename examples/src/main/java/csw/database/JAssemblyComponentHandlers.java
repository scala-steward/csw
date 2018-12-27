package csw.database;

import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.database.client.DatabaseServiceFactory;
import csw.database.client.javadsl.JooqHelper;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import org.jooq.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//DEOPSCSW-615: DB service accessible to CSW component developers
public class JAssemblyComponentHandlers extends JComponentHandlers {

    ActorContext<TopLevelActorMessage> ctx;
    JCswContext cswCtx;
    DatabaseServiceFactory dbFactory;
    DSLContext dsl;

    public JAssemblyComponentHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.ctx = ctx;
        this.cswCtx = cswCtx;

    }

    @Override
    public CompletableFuture<Void> jInitialize() {
        //#dbFactory-access
        dbFactory = new DatabaseServiceFactory(ctx.getSystem());

        dbFactory
                .jMakeDsl(cswCtx.locationService(), "postgres")
                .thenAccept(dsl -> this.dsl = dsl);
        //#dbFactory-access

        //#dbFactory-write-access
        dbFactory
                .jMakeDsl(cswCtx.locationService(), "postgres", "dbWriteUsername", "dbWritePassword")
                .thenAccept(dsl -> this.dsl = dsl);
        //#dbFactory-write-access

        //#dbFactory-test-access
        dbFactory
                .jMakeDsl()
                .thenAccept(dsl -> this.dsl = dsl);
        //#dbFactory-test-access
        return null;
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return null;
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        return null;
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {

        //#dsl-create
        Query createQuery = dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, Name VARCHAR (10) NOT NULL)");
        CompletionStage<Integer> createResultF = createQuery.executeAsync();
        createResultF.thenAccept(result -> System.out.println("Films table created with " + result));
        //#dsl-create

        //#dsl-batch
        String movie_2 = "movie_2";

        Queries queries = dsl.queries(
                dsl.query("INSERT INTO films(name) VALUES (?)", "movie_1"),
                dsl.query("INSERT INTO films(id, name) VALUES (?, ?)", 2, movie_2)
        );

        CompletableFuture<int[]> batchResultF = JooqHelper.executeBatch(queries);
        batchResultF.thenAccept(results ->
                System.out.println("executed queries [" + queries + "] with results [" + Arrays.toString(results) + "]"));
        //#dsl-batch

        //#dsl-fetch
        ResultQuery<Record> selectQuery = dsl.resultQuery("SELECT name FROM films WHERE id = ?", 1);
        CompletableFuture<List<String>> selectResultF = JooqHelper.fetchAsync(selectQuery, String.class);
        selectResultF.thenAccept(names -> System.out.println("Fetched names of films " + names));
        //#dsl-fetch

        //#dsl-function
        Query functionQuery = dsl.query("CREATE FUNCTION inc(val integer) RETURNS integer AS $$\n" +
                "BEGIN\n" +
                "RETURN val + 1;\n" +
                "END; $$\n" +
                "LANGUAGE PLPGSQL;");

        CompletionStage<Integer> functionResultF = functionQuery.executeAsync();
        functionResultF.thenAccept(result -> System.out.println("Function inc created with  " + result));
        //#dsl-function
        return new CommandResponse.Completed(controlCommand.runId());
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {

    }

    @Override
    public void onGoOffline() {

    }

    @Override
    public void onGoOnline() {

    }
}