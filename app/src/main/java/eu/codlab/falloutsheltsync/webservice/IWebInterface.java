package eu.codlab.falloutsheltsync.webservice;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;

import eu.codlab.falloutsheltsync.webservice.models.Authent;
import eu.codlab.falloutsheltsync.webservice.models.Bool;
import eu.codlab.falloutsheltsync.webservice.models.Save;
import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by kevinleperf on 22/08/15.
 */
public interface IWebInterface {

    @PUT("/device")
    void register(Authent authent,
                  Callback<JsonObject> session);

    @POST("/device")
    void authenticate(Authent authent,
                      Callback<JsonObject> session);

    @POST("/device/parent/{parent}")
    void setParent(@Path("parent") String parent,
                   Callback<Bool> result);

    @POST("/push")
    void registerPush(@Field("gcm") String gcm,
                      Callback<HashMap> result);

    @PUT("/save")
    void upload(Save save, Callback<Save> answer);

    @GET("/save/{id}")
    void retrieve(@Path("id") int id,
                  Callback<String> answer);

    @GET("/saves")
    void list(Callback<List<Save>> answer);
}
