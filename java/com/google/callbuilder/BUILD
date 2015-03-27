java_library(
    name = "callbuilder_lib",
    srcs = glob([
        "*.java",
        "style/*.java",
    ]),
    deps = ["//third_party:guava"],
)

java_library(
    name = "callbuilder",
    exported_plugins = [":callbuilder_plugin"],
    visibility = ["//visibility:public"],
    exports = [":callbuilder_lib"],
)

java_plugin(
    name = "callbuilder_plugin",
    processor_class = "com.google.callbuilder.CallBuilderProcessor",
    deps = [
        ":callbuilder_lib",
    ],
)
