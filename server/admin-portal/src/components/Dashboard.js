// src/components/Dashboard.js
import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthContext from '../context/AuthContext';
import axios from 'axios';
import { Box, Button, Grid, Typography, CircularProgress } from '@mui/material';

const Dashboard = () => {
  const { user, logout } = useContext(AuthContext);
  const [tables, setTables] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) {
      navigate('/');
      return;
    }

    const fetchTables = async () => {
      try {
        // Replace with your actual endpoint to get tables
        const response = await axios.get('/api/tables');
        setTables(response.data);
        setLoading(false);
      } catch (error) {
        console.error('Failed to fetch tables:', error);
        setLoading(false);
      }
    };

    fetchTables();
  }, [user, navigate]);

  const handleLogout = () => {
    logout();
  };

  const handleTableClick = (tableName) => {
    navigate(`/table/${tableName}`);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" mt={4}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={4}>
      <Box display="flex" justifyContent="space-between" mb={4}>
        <Typography variant="h4">Admin Dashboard</Typography>
        <Button variant="contained" color="error" onClick={handleLogout}>
          Logout
        </Button>
      </Box>
      <Typography variant="h6" gutterBottom>
        Database Tables
      </Typography>
      <Grid container spacing={2}>
        {tables.map((table) => (
          <Grid item xs={12} sm={6} md={4} key={table}>
            <Button
              variant="outlined"
              fullWidth
              sx={{ height: 100 }}
              onClick={() => handleTableClick(table)}
            >
              {table}
            </Button>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default Dashboard;