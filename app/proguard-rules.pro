# Keep the data model classes Gson deserializes into - their field names are
# matched by reflection against the JSON from the API, so they must survive
# R8's renaming in a release build.
-keep class com.trelegantboutique.admin.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
