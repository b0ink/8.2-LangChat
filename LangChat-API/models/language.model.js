const { DataTypes } = require("sequelize");

module.exports = (sequelize, Sequelize) => {
    const Language = sequelize.define("language", {
        id: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
        },
        name: {
            type: DataTypes.STRING(32),
        },
    });

    return Language;
};
