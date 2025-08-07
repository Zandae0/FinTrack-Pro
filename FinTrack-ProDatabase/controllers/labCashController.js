const { firestore, storage } = require('../config');
const { collection, addDoc, getDocs, doc, updateDoc, deleteDoc } = require('firebase/firestore');
const { ref, uploadBytes, getDownloadURL } = require('firebase/storage');
const multer = require('multer');

// Configure multer for file uploads
const upload = multer({ storage: multer.memoryStorage() });

// Create Lab Cash with photo
exports.createLabCash = [
  upload.single('photo'), // Upload single photo
  async (req, res) => {
    try {
      const { name, inputDate, amount, source } = req.body;
      const file = req.file;

      // Upload photo to Firebase Storage
      const storageRef = ref(storage, `lab_cash/${file.originalname}`);
      const snapshot = await uploadBytes(storageRef, file.buffer);
      const photoURL = await getDownloadURL(snapshot.ref);

      // Save data in Firestore
      const newLabCash = await addDoc(collection(firestore, 'lab_cash'), {
        name,
        inputDate,
        amount,
        source,
        photoURL // Save the photo URL in Firestore
      });

      res.status(201).json({ id: newLabCash.id });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  }
];

// Read Lab Cash
exports.getLabCash = async (req, res) => {
  try {
    const snapshot = await getDocs(collection(firestore, 'lab_cash'));
    const cash = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.status(200).json(cash);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};

// Update Lab Cash
exports.updateLabCash = [
  upload.single('photo'), // Upload single photo
  async (req, res) => {
    try {
      const { id } = req.params;
      const { name, inputDate, amount, source } = req.body;
      const file = req.file;

      let photoURL;
      if (file) {
        // Upload new photo to Firebase Storage
        const storageRef = ref(storage, `lab_cash/${file.originalname}`);
        const snapshot = await uploadBytes(storageRef, file.buffer);
        photoURL = await getDownloadURL(snapshot.ref);
      }

      // Update data in Firestore
      const cashRef = doc(firestore, 'lab_cash', id);
      await updateDoc(cashRef, {
        name,
        inputDate,
        amount,
        source,
        ...(photoURL && { photoURL }) // Only update photoURL if a new file was uploaded
      });

      res.status(200).json({ message: 'Lab cash updated successfully' });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  }
];
// Delete Lab Cash
exports.deleteLabCash = async (req, res) => {
  try {
    const { id } = req.params;
    await deleteDoc(doc(firestore, 'lab_cash', id));
    res.status(200).json({ message: 'Lab cash deleted successfully' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
};