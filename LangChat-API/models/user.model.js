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
            avatar: {
                type: DataTypes.TEXT('long'),
                allowNull: true
            }
        },
        {
            timestamps: false,
        }
    );

    return User;
};
