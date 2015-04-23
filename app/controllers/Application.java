package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Login;
import models.Registration;
import models.utility.AST;
import org.springframework.stereotype.Controller;
import play.Logger;
import play.data.Form;
import play.libs.F;
import play.mvc.Result;
import play.mvc.Results;

import java.util.Iterator;

import static play.mvc.Controller.*;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

@Controller
public class Application {

    public static Result signup() {
        return ok(views.html.guest.signup.render(Form.form(Registration.class)));
    }

    public static Result index() {
        if (isUserLoggedIn()) {
            return ok(views.html.member.index.render(flash("alert"), flash("alert_type")));
        } else {
            return ok(views.html.guest.index.render(flash("alert"), flash("alert_type")));
        }
    }

    public static Result signout() {
        // TODO(Timmay): Inform backend about user log off
        session().clear();
        flash("alert", "Sie haben sich erfolgreich ausgeloggt!");
        flash("alert_type", "success");
        return redirect("/");
    }

    /**
     * WARNING: Contains temporary solutions in order to supply a rudimental error handling
     */
    public static Result register() {
        Form<Registration> form = Form.form(Registration.class).bindFromRequest();

        // TODO(Timmay): signup Form bei fehlgeschlagender Registrierung nochmal rendern!
        // TODO(Timmay): ACHTUNG Binding des Models bei erneutem Versuch, damit bisherige Eingaben nicht geloescht werden

        if (form.hasErrors()) {
            return badRequest(views.html.guest.signup.render(form));
        }

        Registration registration = form.get();

        JsonNode jsonResponse = doRequest("http://localhost:8080/api/v1/register", registration.toJson());

        // temp response and error handling - not to seriously review at this time
        if (jsonResponse.get("email") != null) {
            if (jsonResponse.get("email").asText().equals(registration.email)) {
                flash("alert", "Sie haben sich erfolgreich registriert!");
                flash("alert_type", "success");
                storeValuesInSessionFrom(jsonResponse);
            } else {
                // duplicate code - structure just for temporary error handling
                switch (jsonResponse.get("status").asText()) {
                    default:
                        flash("alert", "Registrierung fehlgeschlagen. " +
                                jsonResponse.get("error").asText() + "\n" +
                                jsonResponse.get("message").asText() + "\n" +
                                "STATUS: " + jsonResponse.get("status").asText());
                        flash("alert_type", "danger");
                }
            }
        } else {
            if (jsonResponse.get("status") != null) {
                // duplicate code - structure just for temporary error handling
                switch (jsonResponse.get("status").asText()) {
                    default:
                        flash("alert", "Registrierung fehlgeschlagen. " +
                                jsonResponse.get("error").asText() + "\n" +
                                jsonResponse.get("message").asText() + "\n" +
                                "STATUS: " + jsonResponse.get("status").asText());
                        flash("alert_type", "danger");
                }
            }
        }


        return redirect("/");
    }

    /**
     * WARNING: Contains temporary solutions in order to supply a rudimental error handling
     */
    public static Result authenticate() {
        // Determine which button of the form was pressed
        String[] postAction = request().body().asFormUrlEncoded().get("action");
        if (postAction == null || postAction.length == 0) {
            return badRequest("You must provide a valid action");
        } else {
            String action = postAction[0];
            switch (action) {
                case "signup":
                    return redirect("/signup");
                case "auth":
                    Logger.info("Action auth received");
                    break;
                default:
                    return badRequest("Bad Action: " + action);
            }
        }

        Form<Login> form = Form.form(Login.class).bindFromRequest();
        Login login = form.get();

        JsonNode jsonResponse = doRequest("http://localhost:8080/api/v1/login", login.toJson());

        // TODO(Timmay): Interpret response and react appropriately
        // temp response and error handling - not to seriously review at this time
        if (jsonResponse.get("email") != null) {
            if (jsonResponse.get("email").asText().equals(login.email)) {
                flash("alert", "Sie haben sich erfolgreich eingeloggt!");
                flash("alert_type", "success");
                storeValuesInSessionFrom(jsonResponse);
            } else {
                // temp alerting if a wild unexpected email error on login appears
                return badRequest("Debug: Login war laut Backend erfolgreich, jedoch stimmen die Email Adressen nicht " +
                                "überein\n" +
                                "Backend: " + jsonResponse.get("email").asText() + "\n" +
                                "Login Model: " + login.email
                );
            }
        } else {
            if (jsonResponse.get("status") != null) {
                switch (jsonResponse.get("status").asText()) {
                    case "404":
                    case "500":
                        flash("alert", "Anmeldung fehlgeschlagen. " +
                                "E-Mail oder Passwort sind nicht korrekt. \n" +
                                "STATUS: " + jsonResponse.get("status").asText());
                        flash("alert_type", "danger");
                        break;
                    default:
                        flash("alert", "Anmeldung fehlgeschlagen. " +
                                jsonResponse.get("error").asText() + "\n" +
                                jsonResponse.get("message").asText() + "\n" +
                                "STATUS: " + jsonResponse.get("status").asText());
                        flash("alert_type", "danger");
                }
            }
        }
        return redirect("/");
    }

    /**
     * Temp. view on map
     */
    public static Result maptest() {
        return ok(views.html.member.maptest.render());
    }

    /**
     * Account view temporarily uses Registration model for filling purposes
     */
    public static Result account() {
        return ok(views.html.member.account.render(
                Form.form(Registration.class)
        ));
    }

    // TODO(Timmay): Implement action for updating users

    /**
     * Stores key value pairs from the json request in the session
     *
     * @param jsonNode json node from which to extract the values
     */
    private static void storeValuesInSessionFrom(JsonNode jsonNode) {
        Iterator<String> nodeIt = jsonNode.fieldNames();

        while (nodeIt.hasNext()) {
            String field = nodeIt.next();
            // store key value pairs from request in session
            session(field, jsonNode.get(field).asText());
        }
    }

    /**
     * Runs procedures neccessary to perform a request and returns the response
     *
     * @param url      URL to call
     * @param jsonNode Acutal request content
     * @return Response of the request
     */
    private static JsonNode doRequest(String url, JsonNode jsonNode) {
        int responseTimeoutInMs = 10000;

        F.Promise<JsonNode> jsonPromise = AST.preparedJson(url).post(jsonNode);

        // TODO(Timmay): Create general pages for no-OK (200) responses and implement proper handling
        return jsonPromise.get(responseTimeoutInMs);
    }

    /**
     * Checks whether the user is logged in or not
     *
     * @return true, if user is logged in
     */
    public static boolean isUserLoggedIn() {
        return session("token") != null;
    }
}
