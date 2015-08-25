package eu.codlab.falloutsheltsync.webservice.models;

/**
 * Created by kevinleperf on 11/07/15.
 */
public class Authent {
    public String uuid;
    public String pass;

    public Authent(String uuid, String pass) {
        this.uuid = uuid;
        this.pass = pass;
    }
}
