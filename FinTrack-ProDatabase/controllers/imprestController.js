const { firestore, storage } = require('../config');
const { collection, addDoc, getDocs, doc, updateDoc, deleteDoc } = require('firebase/firestore');
const { ref, uploadBytes, getDownloadURL } = require('firebase/storage');
const multer = require('multer');
// Configure multer for file uploads
const upload = multer({ storage: multer.memoryStorage() });

//dadad

// Create Imprest Fund with photo
exports.createImprestFund = [
  upload.single('photo'), // Upload single photo
  async (req, res) => {
    try {
      const { name, inputDate, purpose, transactionDate, amount, source, pic } = req.body;
      const file = req.file;

      // Upload photo to Firebase Storage
      const storageRef = ref(storage, `imprest_funds/${file.originalname}`);
      const snapshot = await uploadBytes(storageRef, file.buffer);
      const photoURL = await getDownloadURL(snapshot.ref);

      // Save data in Firestore
      const newImprestFund = await addDoc(collection(firestore, 'imprest_funds'), {
        name,
        inputDate,
        purpose,
        transactionDate,
        amount,
        source,
        pic,
        photoURL // Save the photo URL in Firestore
      });

      res.status(201).json({ id: newImprestFund.id });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  }
];

// Read
exports.getImprestFunds = async (req, res) => {
  try {
    const snapshot = await getDocs(collection(firestore, 'imprest_funds'));
    const funds = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.status(200).json(funds);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// Update
exports.updateImprestFund = [
  upload.single('photo'), // Upload single photo
  async (req, res) => {
    try {
      const { id } = req.params;
      const { name, inputDate, purpose, transactionDate, amount, source, pic } = req.body;
      const file = req.file;

      let photoURL;
      if (file) {
        // Upload new photo to Firebase Storage
        const storageRef = ref(storage, `imprest_funds/${file.originalname}`);
        const snapshot = await uploadBytes(storageRef, file.buffer);
        photoURL = await getDownloadURL(snapshot.ref);
      }

      // Update data in Firestore
      const fundRef = doc(firestore, 'imprest_funds', id);
      await updateDoc(fundRef, {
        name,
        inputDate,
        purpose,
        transactionDate,
        amount,
        source,
        pic,
        ...(photoURL && { photoURL }) // Only update photoURL if a new file was uploaded
      });

      res.status(200).json({ message: 'Imprest fund updated successfully' });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  }
];

// Delete
exports.deleteImprestFund = async (req, res) => {
  try {
    const { id } = req.params;
    await deleteDoc(doc(firestore, 'imprest_funds', id));
    res.status(200).json({ message: 'Imprest fund deleted successfully' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};
