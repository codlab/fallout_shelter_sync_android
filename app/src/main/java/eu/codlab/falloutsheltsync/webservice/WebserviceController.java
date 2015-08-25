package eu.codlab.falloutsheltsync.webservice;

import android.util.Base64;

import com.google.gson.JsonObject;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import de.greenrobot.event.EventBus;
import eu.codlab.falloutsheltsync.ApplicationController;
import eu.codlab.falloutsheltsync.webservice.models.Authent;
import eu.codlab.falloutsheltsync.webservice.models.Bool;
import eu.codlab.falloutsheltsync.webservice.models.Save;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevinleperf on 11/07/15.
 */
public class WebserviceController {
    public interface IAuthenticate {
        void onSessionOk(String session);

        void onSessionError();
    }

    public interface IRegister {
        void onRegisterOk(String session);

        void onRegisterError();
    }

    public interface IPost {
        void onSavePost(Save save);

        void onSavePostError();
    }

    public interface IGet {
        void onSavesRetrieved(List<Save> saves);

        void onSavesError();
    }

    public interface IParent {
        void onParentSet();

        void onParentSetError();
    }

    public interface IRetrieve {
        void onRetrieve(File file);

        void onRetrieveError();
    }


    private static WebserviceController _instance = new WebserviceController();

    public static WebserviceController getInstance() {
        return _instance;
    }

    private RestAdapter _adapter;
    private IWebInterface _web_interface;
    private WsseToken _token;

    private WebserviceController() {
        recreateRestAdapter();
        EventBus.getDefault().register(this);
    }

    private void recreateRestAdapter() {
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint("http://fallout.codlab.eu")
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        if (_token != null) {
                            request.addHeader(WsseToken.HEADER_AUTHORIZATION, _token.getAuthorizationHeader());
                            request.addHeader(WsseToken.HEADER_WSSE, _token.getWsseHeader());
                        }
                    }
                });

        _adapter = builder.build();
        _web_interface = _adapter.create(IWebInterface.class);

    }

    public void register(final IRegister callback) {
        final Authent authent = ApplicationController.getInstance().getAuthenticationObject();

        _web_interface.register(authent, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject jsonObject, Response response) {
                if (jsonObject.has("session")) {
                    _token = new WsseToken(jsonObject.getAsJsonPrimitive("session").getAsString(),
                            hashPassword(null, authent.pass));
                    callback.onRegisterOk(jsonObject.getAsJsonPrimitive("session").getAsString());
                } else {
                    callback.onRegisterError();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                callback.onRegisterError();
            }
        });
    }

    public void authenticate(final IAuthenticate callback) {
        final Authent authent = ApplicationController.getInstance().getAuthenticationObject();

        _web_interface.authenticate(authent, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject jsonObject, Response response) {
                if (jsonObject.has("session")) {
                    _token = new WsseToken(jsonObject.getAsJsonPrimitive("session").getAsString(),
                            hashPassword(null, authent.pass));
                    callback.onSessionOk(_token.getPassword());
                } else {
                    callback.onSessionError();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                callback.onSessionError();
            }
        });
    }

    public void setParent(final String parent, final IParent callback) {
        setParent(parent, callback, true);
    }

    public void setParent(final String parent, final IParent callback, final boolean try_authenticate) {
        Callback<Bool> result = new Callback<Bool>() {
            @Override
            public void success(Bool res, Response response) {
                int code = response.getStatus();
                if ((code & 200) == 200) {
                    callback.onParentSet();
                } else {
                    onError(code);
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }

            private void onError(int code) {
                if (try_authenticate) {
                    if (code == 401) {
                        authenticate(new IAuthenticate() {
                            @Override
                            public void onSessionOk(String session) {
                                setParent(parent, callback, false);
                            }

                            @Override
                            public void onSessionError() {
                                callback.onParentSetError();
                            }
                        });
                    } else if (code == 403) {
                        register(new IRegister() {
                            @Override
                            public void onRegisterOk(String session) {
                                setParent(parent, callback, false);
                            }

                            @Override
                            public void onRegisterError() {
                                callback.onParentSetError();
                            }
                        });
                    }
                }
            }
        };

        _web_interface.setParent(parent, result);
    }

    public void list(final IGet callback) {
        list(callback, true);
    }

    private void list(final IGet callback, final boolean try_authenticate) {

        Callback<List<Save>> first_call = new Callback<List<Save>>() {
            @Override
            public void success(List<Save> answer, Response response) {
                int code = response.getStatus();
                if ((code & 200) == 200) {
                    callback.onSavesRetrieved(answer);
                } else {
                    onError(code);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                int code = error.getResponse() != null ? error.getResponse().getStatus() : 0;
                callback.onSavesError();

                onError(code);
            }

            private void onError(int code) {
                if (try_authenticate) {
                    if (code == 401) {
                        authenticate(new IAuthenticate() {
                            @Override
                            public void onSessionOk(String session) {
                                list(callback, false);
                            }

                            @Override
                            public void onSessionError() {
                                callback.onSavesError();
                            }
                        });
                    } else if (code == 403) {
                        register(new IRegister() {
                            @Override
                            public void onRegisterOk(String session) {
                                list(callback, false);
                            }

                            @Override
                            public void onRegisterError() {
                                callback.onSavesError();
                            }
                        });
                    }
                }
            }
        };

        _web_interface.list(first_call);
    }

    public void postSave(String content, int slot, String info,
                         final IPost callback) {
        final Save message_to_post = new Save();
        message_to_post.content = content;
        message_to_post.slot = slot;
        message_to_post.info = info;

        postSave(message_to_post, callback, true);
    }

    private void postSave(final Save message_to_post,
                          final IPost callback, final boolean try_authenticate) {

        Callback<Save> first_call = new Callback<Save>() {
            @Override
            public void success(Save answer, Response response) {
                int code = response.getStatus();
                android.util.Log.d("answer", "" + answer);
                if (answer != null) {
                    callback.onSavePost(answer);
                } else {
                    onError(code);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
                int code = error.getResponse() != null ? error.getResponse().getStatus() : 0;
                callback.onSavePostError();

                onError(code);
            }

            private void onError(int code) {
                if (try_authenticate) {
                    if (code == 401) {
                        authenticate(new IAuthenticate() {
                            @Override
                            public void onSessionOk(String session) {
                                postSave(message_to_post, callback, false);
                            }

                            @Override
                            public void onSessionError() {
                                callback.onSavePostError();
                            }
                        });
                    } else if (code == 403) {
                        register(new IRegister() {
                            @Override
                            public void onRegisterOk(String session) {
                                postSave(message_to_post, callback, false);
                            }

                            @Override
                            public void onRegisterError() {
                                callback.onSavePostError();
                            }
                        });
                    }
                }
            }
        };

        _web_interface.upload(message_to_post, first_call);
    }

    public static String hashPassword(String salt, String clearPassword) {
        String hash = "";
        try {
            String salted = null;
            if (salt == null || "".equals(salt)) {
                salted = clearPassword;
            } else {
                salted = clearPassword + "{" + salt + "}";
            }
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte sha[] = md.digest(salted.getBytes());
            for (int i = 1; i < 5000; i++) {
                byte c[] = new byte[sha.length + salted.getBytes().length];
                System.arraycopy(sha, 0, c, 0, sha.length);
                System.arraycopy(salted.getBytes(), 0, c, sha.length, salted.getBytes().length);
                sha = md.digest(c);
            }
            hash = new String(Base64.encode(sha, Base64.NO_WRAP));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
