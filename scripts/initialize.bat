curl "http://localhost:9000/user" -H "Content-Type: application/json" -H "Origin: http://localhost:4200" ^
    --data-raw "{\"firstName\":\"Marco\",\"lastName\":\"Paggioro\",\"birthDate\":\"1998-06-01\",\"email\":\"marco.paggioro@studenti.unipegaso.it\",\"encryptedPassword\":\"ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff\"}"

curl "http://localhost:9000/user" -H "Content-Type: application/json" -H "Origin: http://localhost:4200" ^
    --data-raw "{\"firstName\":\"Mario\",\"lastName\":\"Rossi\",\"birthDate\":\"1990-10-27\",\"email\":\"mario@rossi.it\",\"encryptedPassword\":\"ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff\"}"

curl "http://localhost:9000/user" -H "Content-Type: application/json" -H "Origin: http://localhost:4200" ^
    --data-raw "{\"firstName\":\"Giovanni\",\"lastName\":\"Pirozzi\",\"birthDate\":\"1996-05-15\",\"email\":\"giovanni@pirozzi.it\",\"encryptedPassword\":\"ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff\"}"