var Localize = require("localize-with-spreadsheet");
var transformer = Localize.fromGoogleSpreadsheet("1bG9ZNStlco4Ruaj5nVt40WWTG5o2RH3xi-CNsU0nPwo", '*');
transformer.setKeyCol('key');

transformer.save("app/src/main/res/values/strings.xml", { valueCol: "en", format: "android" });
transformer.save("app/src/main/res/values-fr/strings.xml", { valueCol: "fr", format: "android" });
transformer.save("app/src/main/res/values-en/strings.xml", { valueCol: "en", format: "android" });
