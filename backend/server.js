require('dotenv').config();
const app = require('./app');

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`🚀  LSMS Backend running at http://localhost:${PORT}`);
  console.log(`🌐  Frontend served at   http://localhost:${PORT}/index.html`);
  console.log(`📡  API base:            http://localhost:${PORT}/api`);
});
