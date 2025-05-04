// src/components/TableView.js
import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AuthContext from '../context/AuthContext';
import axios from 'axios';
import {
  Box,
  Button,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
  Snackbar,
  Alert,
} from '@mui/material';
import { Edit, Delete, Add } from '@mui/icons-material';

const TableView = () => {
  const { tableName } = useParams();
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  const [data, setData] = useState([]);
  const [columns, setColumns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openAddDialog, setOpenAddDialog] = useState(false);
  const [openEditDialog, setOpenEditDialog] = useState(false);
  const [currentRow, setCurrentRow] = useState(null);
  const [formData, setFormData] = useState({});
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success',
  });

  useEffect(() => {
    if (!user) {
      navigate('/');
      return;
    }

    const fetchTableData = async () => {
      try {
        // Replace with your actual endpoint to get table data
        const response = await axios.get(`/api/tables/${tableName}`);
        setData(response.data.rows);
        setColumns(response.data.columns);
        setLoading(false);
      } catch (error) {
        console.error(`Failed to fetch ${tableName} data:`, error);
        setLoading(false);
      }
    };

    fetchTableData();
  }, [tableName, user, navigate]);

  const handleAddClick = () => {
    const initialFormData = {};
    columns.forEach((col) => {
      initialFormData[col] = '';
    });
    setFormData(initialFormData);
    setOpenAddDialog(true);
  };

  const handleEditClick = (row) => {
    setCurrentRow(row);
    setFormData({ ...row });
    setOpenEditDialog(true);
  };

  const handleDeleteClick = async (id) => {
    try {
      // Replace with your actual delete endpoint
      await axios.delete(`/api/tables/${tableName}/${id}`);
      setData(data.filter((row) => row.id !== id));
      showSnackbar('Row deleted successfully', 'success');
    } catch (error) {
      console.error('Failed to delete row:', error);
      showSnackbar('Failed to delete row', 'error');
    }
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value,
    });
  };

  const handleAddSubmit = async (e) => {
    e.preventDefault();
    try {
      // Replace with your actual add endpoint
      const response = await axios.post(`/api/tables/${tableName}`, formData);
      setData([...data, response.data]);
      setOpenAddDialog(false);
      showSnackbar('Row added successfully', 'success');
    } catch (error) {
      console.error('Failed to add row:', error);
      showSnackbar('Failed to add row', 'error');
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    try {
      // Replace with your actual update endpoint
      await axios.put(
        `/api/tables/${tableName}/${currentRow.id}`,
        formData
      );
      setData(
        data.map((row) => (row.id === currentRow.id ? { ...formData } : row))
      );
      setOpenEditDialog(false);
      showSnackbar('Row updated successfully', 'success');
    } catch (error) {
      console.error('Failed to update row:', error);
      showSnackbar('Failed to update row', 'error');
    }
  };

  const showSnackbar = (message, severity) => {
    setSnackbar({ open: true, message, severity });
  };

  const handleSnackbarClose = () => {
    setSnackbar({ ...snackbar, open: false });
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
      <Typography variant="h4" gutterBottom>
        {tableName} Table
      </Typography>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell key={column}>{column}</TableCell>
              ))}
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={columns.length + 1}>
                <IconButton onClick={handleAddClick}>
                  <Add /> Add New Row
                </IconButton>
              </TableCell>
            </TableRow>
            {data.map((row) => (
              <TableRow key={row.id}>
                {columns.map((column) => (
                  <TableCell key={`${row.id}-${column}`}>
                    {row[column]}
                  </TableCell>
                ))}
                <TableCell>
                  <IconButton onClick={() => handleEditClick(row)}>
                    <Edit color="primary" />
                  </IconButton>
                  <IconButton onClick={() => handleDeleteClick(row.id)}>
                    <Delete color="error" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Add Dialog */}
      <Dialog open={openAddDialog} onClose={() => setOpenAddDialog(false)}>
        <DialogTitle>Add New Row</DialogTitle>
        <form onSubmit={handleAddSubmit}>
          <DialogContent>
            {columns.map((column) => (
              <TextField
                key={column}
                margin="dense"
                name={column}
                label={column}
                fullWidth
                variant="outlined"
                value={formData[column] || ''}
                onChange={handleFormChange}
                sx={{ mb: 2 }}
              />
            ))}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenAddDialog(false)}>Cancel</Button>
            <Button type="submit" variant="contained" color="primary">
              Add
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={openEditDialog} onClose={() => setOpenEditDialog(false)}>
        <DialogTitle>Edit Row</DialogTitle>
        <form onSubmit={handleEditSubmit}>
          <DialogContent>
            {columns.map((column) => (
              <TextField
                key={column}
                margin="dense"
                name={column}
                label={column}
                fullWidth
                variant="outlined"
                value={formData[column] || ''}
                onChange={handleFormChange}
                sx={{ mb: 2 }}
              />
            ))}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenEditDialog(false)}>Cancel</Button>
            <Button type="submit" variant="contained" color="primary">
              Update
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default TableView;