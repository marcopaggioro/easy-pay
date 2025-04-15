curl "http://localhost:9000/user" -H "Content-Type: application/json" -H "Origin: http://localhost:4200" ^
    --data-raw "{\"firstName\":\"Marco\",\"lastName\":\"Paggioro\",\"birthDate\":\"1998-06-01\",\"email\":\"marco.paggioro@studenti.unipegaso.it\",\"encryptedPassword\":\"2bbe0c48b91a7d1b8a6753a8b9cbe1db16b84379f3f91fe115621284df7a48f1cd71e9beb90ea614c7bd924250aa9e446a866725e685a65df5d139a5cd180dc9\"}"

curl "http://localhost:9000/user" -H "Content-Type: application/json" -H "Origin: http://localhost:4200" ^
    --data-raw "{\"firstName\":\"Mario\",\"lastName\":\"Rossi\",\"birthDate\":\"1990-10-27\",\"email\":\"mario@rossi.it\",\"encryptedPassword\":\"2bbe0c48b91a7d1b8a6753a8b9cbe1db16b84379f3f91fe115621284df7a48f1cd71e9beb90ea614c7bd924250aa9e446a866725e685a65df5d139a5cd180dc9\"}"

curl "http://localhost:9000/user" -H "Content-Type: application/json" -H "Origin: http://localhost:4200" ^
    --data-raw "{\"firstName\":\"Luca\",\"lastName\":\"Moro\",\"birthDate\":\"1996-05-15\",\"email\":\"luca@moro.it\",\"encryptedPassword\":\"2bbe0c48b91a7d1b8a6753a8b9cbe1db16b84379f3f91fe115621284df7a48f1cd71e9beb90ea614c7bd924250aa9e446a866725e685a65df5d139a5cd180dc9\"}"