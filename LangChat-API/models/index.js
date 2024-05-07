const { Sequelize } = require("sequelize");
const config = require("../config.json");

const sequelize = new Sequelize(config.DB_DATABASE, config.DB_USER, config.DB_PASS, {
    host: config.DB_HOST,
    port: config.SQL_PORT,
    dialect: "mysql",
    sync: false,
    dialectOptions: {
        // hotfix for my xampp installation of mysql/phpmyadmin
        socketPath: "/Applications/XAMPP/xamppfiles/var/mysql/mysql.sock"
    },
});

const db = {};

db.Sequelize = Sequelize;
db.sequelize = sequelize;

db.users = require("./user.model.js")(sequelize, Sequelize);

// THIS WILL DROP TABLES
// sequelize.sync({force: true});

module.exports = db;