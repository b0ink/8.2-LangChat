const { DataTypes } = require("sequelize");

module.exports = (sequelize, Sequelize) => {

    const Translation = sequelize.define("translation", {
        id: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
        },
        message_id: {
            type: DataTypes.INTEGER,
            references: {
                model: "messages",
                key: "id",
            },
        },
        language: {
            type: DataTypes.STRING(32),
        },
        message: {
            type: DataTypes.STRING(255),
        },
    });


    Translation.belongsTo(sequelize.models.conversation, { foreignKey: "message_id", as:'message' });

    return Translation;
};
