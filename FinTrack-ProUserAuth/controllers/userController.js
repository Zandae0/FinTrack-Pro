const { getFirestore, collection, getDocs, updateDoc, doc } = require('firebase/firestore');
const { db } = require('../firebaseClient');

const getAllUsers = async (req, res) => {
  const usersCollection = collection(db, 'users');
  try {
    const snapshot = await getDocs(usersCollection);
    const users = snapshot.docs.map(doc => doc.data());
    res.status(200).json(users);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

const changeUserRole = async (req, res) => {
  const { uid } = req.params;
  const { status } = req.body;

  if (status !== 'admin' && status !== 'user') {
    return res.status(400).json({ error: 'Invalid status' });
  }

  const userDoc = doc(db, 'users', uid);
  try {
    await updateDoc(userDoc, { status });
    res.status(200).json({ message: 'User role updated successfully' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

module.exports = { getAllUsers, changeUserRole };
