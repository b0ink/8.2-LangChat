const { DataTypes } = require("sequelize");

module.exports = (sequelize, Sequelize) => {
    const Conversation = sequelize.define("conversation", {
        id: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
        },
    });

    return Conversation;
};
