const express = require('express');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const { auth, db } = require('./firebaseClient');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());
app.use('/auth', authRoutes);
app.use('/users', userRoutes);

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
