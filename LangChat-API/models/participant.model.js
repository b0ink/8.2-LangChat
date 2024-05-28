const { DataTypes } = require("sequelize");

module.exports = (sequelize, Sequelize) => {
    const Participant = sequelize.define("participant", {
        id: {
            type: DataTypes.INTEGER,
            primaryKey: true,
            autoIncrement: true,
        },
        conversation_id: {
            type: DataTypes.INTEGER,
            references: {
                model: "conversations",
                key: "id",
            },
        },
        user_id: {
            type: DataTypes.INTEGER,
            references: {
                model: "users",
                key: "id",
            },
        },
        // Overrides user's base preferred language per conversation
        preferredLanguage: {
            type: DataTypes.STRING(32),
            allowNull: true,
            defaultValue: null,
        },
        isAdmin: {
            type: DataTypes.BOOLEAN,
            defaultValue: false,
        },
    });

    Participant.belongsTo(sequelize.models.conversation, { foreignKey: "conversation_id", as: "conversation" });
    Participant.belongsTo(sequelize.models.user, { foreignKey: "user_id", as: "user" });

    return Participant;
};
