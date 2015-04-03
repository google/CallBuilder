# Copyright 2015 Google Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ANNOTATIONS = ["BuilderField.java", "CallBuilder.java"]

java_library(
    name = "processor_lib",
    srcs = glob(["*.java"], exclude = ANNOTATIONS),
    deps = [
        ":annotations",
        "//java/com/google/callbuilder/util",
        "//third_party:jsr305",
    ],
    visibility = ["//visibility:public"],
)

java_library(
    name = "annotations",
    srcs = ANNOTATIONS,
)

java_library(
    name = "callbuilder",
    exported_plugins = [":callbuilder_plugin"],
    exports = [":annotations"],
    visibility = ["//visibility:public"],
)

java_plugin(
    name = "callbuilder_plugin",
    processor_class = "com.google.callbuilder.CallBuilderProcessor",
    deps = [
        ":processor_lib",
    ],
)
