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

package(default_testonly = 1)

java_test(
    name = "CallBuilderTest",
    srcs = ["CallBuilderTest.java"],
    deps = [
        "//java/com/google/callbuilder",
        "//third_party:guava",
        "//third_party:junit4",
    ],
)

# Makes sure that we can use functionality given in the style library.
java_test(
    name = "WithStyleTest",
    srcs = ["WithStyleTest.java"],
    deps = [
        "//java/com/google/callbuilder",
        "//java/com/google/callbuilder/style",
        "//third_party:guava",
        "//third_party:junit4",
    ],
)

java_test(
    name = "UnificationTest",
    srcs = ["UnificationTest.java"],
    deps = [
        "//java/com/google/callbuilder:processor_lib",
        "//third_party:guava",
        "//third_party:junit4",
    ],
)
