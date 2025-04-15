docker run --rm -v %cd%/backend:/root sbtscala/scala-sbt:eclipse-temurin-23.0.2_7_1.10.11_3.3.5 sbt clean compile Docker/stage

docker compose up --detach --build

timeout /t 10
cd "scripts"
call initialize.bat