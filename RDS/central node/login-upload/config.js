// This file contains private configuration details.
// Do not add it to your Git repository.

exports.rdd_directory_path = '/home/simone/UniTn/IEHR/IEHR-server/RDS/central-node/login-upload/RDD/'


//database connection
const client = {
  user: 'simone',
  host: 'localhost',
  database: 'simodb',
  password: 'miapsql-8',
  port: 5432
}

const admin = {
  user: 'admin',
  password: 'admin'
}

const endpoint = {
  host: 'localhost',
  port: '5000'
}

exports.connectionInfo = client
exports.admin = admin
exports.endpoint = endpoint
