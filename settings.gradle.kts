rootProject.name = "IceStom"

include(
    ":icestom",
    ":stomtrack",
    ":linefold"
)

project(":icestom").projectDir = file("projects/icestom")
project(":stomtrack").projectDir = file("projects/stomtrack")
project(":linefold").projectDir = file("projects/linefold")