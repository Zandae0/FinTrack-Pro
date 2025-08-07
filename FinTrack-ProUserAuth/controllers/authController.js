const { signInWithEmailAndPassword, createUserWithEmailAndPassword, sendPasswordResetEmail, signOut } = require('firebase/auth');
const { doc, setDoc } = require('firebase/firestore');
const { auth, db } = require('../firebaseClient');
const { adminUsername, adminPassword } = require('../config');
//adada
const login = async (req, res) => {
    const { email, password, username } = req.body;
  
    try {
      // Cek kredensial admin
      if (username === adminUsername && password === adminPassword) {
        // Cek apakah pengguna yang login adalah admin
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;
        const idToken = await user.getIdToken(); // Mendapatkan token
  
        res.status(200).json({ uid: user.uid, email: user.email, idToken, role: 'admin' });
        return;
      }
  
      // Cek kredensial pengguna biasa
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;
      const idToken = await user.getIdToken(); // Mendapatkan token
  
      res.status(200).json({ uid: user.uid, email: user.email, idToken, role: 'user' });
    } catch (error) {
      res.status(400).json({ error: error.message });
    }
  };

const register = async (req, res) => {
  const { email, password, name, username, role } = req.body;

  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    const user = userCredential.user;

    await setDoc(doc(db, 'users', user.uid), {
      uid: user.uid,
      name,
      username,
      email,
      role,
      status: 'user' // Default status
    });

    res.status(201).json({ uid: user.uid, email: user.email });
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
};

const forgotPassword = async (req, res) => {
  const { email } = req.body;

  try {
    await sendPasswordResetEmail(auth, email);
    res.status(200).json({ message: 'Password reset email sent' });
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
};

const logout = async (req, res) => {
  try {
    await signOut(auth);
    res.status(200).json({ message: 'Successfully logged out' });
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
};

module.exports = { login, register, forgotPassword, logout };
