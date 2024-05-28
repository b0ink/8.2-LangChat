const multer = require("multer");
const path = require("path");

// Configure storage for multer
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, "audio/"); // Specify the destination directory for uploaded files
    },
    filename: function (req, file, cb) {
        cb(null, file.originalname);
        // cb(null, file.fieldname + '-' + Date.now() + path.extname(file.originalname)); // Rename the file
    },
});

// Initialize upload middleware
const upload = multer({ storage: storage });

module.exports = upload;
