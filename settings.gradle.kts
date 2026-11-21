rootProject.name = "IceStom"

include(
    ":icestom",
    ":stomtrack",
    ":linefold",
    ":protocol",
    ":tracklua",
    ":lua"
)

project(":icestom").projectDir = file("projects/icestom")
project(":stomtrack").projectDir = file("projects/stomtrack")
project(":linefold").projectDir = file("projects/linefold")
project(":protocol").projectDir = file("projects/protocol")
project(":tracklua").projectDir = file("projects/tracklua")
project(":lua").projectDir = file("projects/lua")