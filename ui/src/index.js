import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { GoogleOAuthProvider } from '@react-oauth/google';

const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
    <GoogleOAuthProvider clientId="1089386037845-h6ih1k6390g3mqv964hedgojsqj9bsnv.apps.googleusercontent.com">
      <App />
    </GoogleOAuthProvider>
);
