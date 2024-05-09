const db = require("../models");
const User = db.users;

const { secretKey } = require("../config.json");
const Joi = require("joi");
const jwt = require("jsonwebtoken");

const bcrypt = require("bcrypt");
const saltRounds = 10;

const Utility = require("./Utility");

// Create and Save a new User
exports.create = async (req, res) => {
    const user = { ...req.body };
    console.log(req.body);
    const registerSchema = Joi.object({
        username: Joi.string().alphanum().min(3).max(30).required(),
        // fullname: Joi.string().min(3).max(30).required(),
        email: Joi.string().email().required(),
        confirmEmail: Joi.ref("email"),
        password: Joi.string().pattern(new RegExp("^[a-zA-Z0-9]{3,30}$")).required(),
        confirmPassword: Joi.ref("password"),
        // mobile: Joi.string().pattern(new RegExp("^\\d{10}$")).required(),
    })
        .with("email", "confirmEmail")
        .with("password", "confirmPassword");

    const { error, value } = registerSchema.validate(req.body);

    if (error) {
        const errorMessage = error.details[0].message;
        return res.status(200).json({ message: errorMessage ? errorMessage : "An unknown error occurred.", status: "400" });
    }

    //TODO: check to see if any accounts exist with this username/email

    const EncryptedPasword = await bcrypt.hashSync(user.password, saltRounds);

    const newUser = {
        username: user.username,
        email: user.email,
        password: EncryptedPasword,
        mobile: user.mobile,
    };

    console.log(newUser);

    User.create(newUser)
        .then((data) => {
            res.json({
                message: "Succesfully created account.",
                status: 200,
            });
        })
        .catch((err) => {
            res.status(500).send({
                message: err.message || "Some error occurred while creating user.",
            });
        });
};

exports.findOne = async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    const user = await User.findOne({ where: { username } });

    if (!user) {
        console.log("no user!");
        return res.status(400).json({ message: "Invalid username/password" });
    }

    const passwordsMatch = await bcrypt.compareSync(password, user.password);

    if (!passwordsMatch) {
        console.log("wrong password");
        return res.status(400).json({ message: "Invalid username/password" });
    }

    const token = jwt.sign({ id: user.id, username: user.username }, secretKey, { expiresIn: "24h" });
    console.log("logged in");
    return res.header("Authorization", token).json({ message: "Login successful", token });
};

exports.findConversations = async (req, res) => {
    // const user = req.user;
    const user = {
        id: 1,
        username: "bob"
    };

    const conversationIds = await Utility.GetUsersConversations(user.id);

    //TODO: to display "unread" messages, store a local time the last time a conversation was opened
    //TODO: -> when loading conversations, compare that opened time with the most recent message in the conversation, if its newer, display notification dot

    /*
       Conversations: [
            {
                id,
                last_message: {
                    id,
                    sender_id,
                    message: "Hello world",
                    created_at: Date.now()
                }
            },
            {
                id,
                last_message: {
                    id,
                    sender_id,
                    message: "Hello world",
                    created_at: Date.now()
                }
            }
        ]
    */

    let Conversations = [];

    for(let conv_id of conversationIds){
        const participants = await Utility.GetConversationParticipants(conv_id);
        const lastMessage = await Utility.GetMostRecentConversationMessage(conv_id);
        
        Conversations.push({
            id: conv_id,
            // Dont include requesting user as the participant
            // Will be used to display the "name" of the conversation (more than 1 participant will be a group chat)
            participants: [...participants.filter(p=>p.user.username!==user.username)],
            lastMessage:lastMessage
        });
    }

    return res.json(Conversations);
};
