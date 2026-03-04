import sys

pom_path = "community-service/pom.xml"
with open(pom_path, "r") as f:
    content = f.read()

dep = """
<!-- Tests -->
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-test</artifactId>
<scope>test</scope>
</dependency>
<dependency>
<groupId>io.projectreactor</groupId>
<artifactId>reactor-test</artifactId>
<scope>test</scope>
</dependency>
"""

new_content = content.replace("</dependencies>", dep + "\t</dependencies>")
with open(pom_path, "w") as f:
    f.write(new_content)

print("Dependencies added.")
