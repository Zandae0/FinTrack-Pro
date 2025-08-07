const express = require('express');
const bodyParser = require('body-parser');
const imprestRoutes = require('./routes/imprestRoutes');
const labCashRoutes = require('./routes/labCashRoutes');

const app = express();

app.use(bodyParser.json());
app.use('/imprest', imprestRoutes);
app.use('/lab-cash', labCashRoutes); 

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
