const { DataTypes } = require("sequelize");

module.exports = (sequelize, Sequelize) => {
    const User = sequelize.define(
        "user",
        {
            id: {
                type: DataTypes.INTEGER,
                primaryKey: true,
                autoIncrement: true,
            },
            username: {
                type: DataTypes.STRING(32),
                allowNull: false,
            },
            email: {
                type: DataTypes.STRING(64),
                allowNull: false,
            },
            password: {
                type: DataTypes.STRING(256),
                allowNull: false,
            },
            defaultPreferredLanguage: {
                type: DataTypes.STRING(32),
                allowNull: false,
                defaultValue: "English"
            },
        },
        {
            timestamps: false,
        }
    );

    return User;
};
