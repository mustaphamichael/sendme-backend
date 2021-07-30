# sendme-backend-challenge

SendMe is an application for receiving and sending money (a demo financial service app). The backend service
implementation using Scala, Akka-Http, Slick, and PostgreSQL.

#### (Transaction Runner)

Create APIs for a set of Financial transaction, full CRUD with a mini deployment pipeline

- Create Auth APIs for signup, login, sign-out with standard User Models
- Each user should have a Global balance, and an object to show incoming and outgoing transactions (Style the User
  Object in the best possible way you can)
- Mock transactions to create a financial ledger (Send Money, Add Money, Please Note that you do not have to integrate
  with any payment platform, just mock transactions by adding and subtracting to the Global User balance and also adding
  to the Transaction List)
- Create a Production, Development and Test pipeline with different databases and deploy to preferred server (AWS,
  Heroku or GCP)

#### Setup

The `/data` directory contains the SQL Data Definition scripts, Docker compose file end environmental variables.
Checkout the `secret.env.example` for required variables.

#### TODO

- Unit and integration test
- API Documentation
- CI/CD pipeline