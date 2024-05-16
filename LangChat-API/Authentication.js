const jwt = require("jsonwebtoken");

const { secretKey } = require("./config.json");

module.exports.authenticateToken = (req, res, next) => {
    const authHeader = req.headers["authorization"].trim();
    console.log(authHeader);
    let token = authHeader;
    if (authHeader.indexOf(" ") != -1) {
        token = authHeader && authHeader.split(" ")[1];
    }

    if (!token) return res.status(401).json({ message: "Unauthorized" });

    jwt.verify(token, secretKey, (err, user) => {
        if (err) return res.status(403).json({ message: "Forbidden" });
        req.user = user;
        next();
    });
}
