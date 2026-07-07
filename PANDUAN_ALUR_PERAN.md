# PANDUAN ALUR KERJA PERAN (ROLE WORKFLOW ROADMAP)

Dokumen ini berisi panduan alur kerja langkah demi langkah untuk setiap peran (role) di aplikasi **FabricationPro**, lengkap dengan kredensial masuk dan fitur yang digunakan.

---

## 1. Peran: Operator (Lini Produksi)

Operator bertanggung jawab terhadap aktivitas perakitan unit panel, pengujian kualitas (QC), perbaikan unit gagal, dan pengemasan awal.

### 🔐 Kredensial Login
* **Pilih Peran**: Operator (Tombol Kiri)
* **User ID**: `OP123`
* **Access Code**: `123456`

### 🔄 Alur Langkah Kerja
1. **Autentikasi**: Isi kredensial Operator dan klik **Authenticate** untuk masuk ke beranda.
2. **Dashboard**: Memantau log aktivitas perakitan real-time pada *Live Activity Feed*.
3. **Perakitan Unit**:
   - Masuk ke tab **Produksi** ➔ Klik **Mulai Perakitan Baru**.
   - Ikuti 5 langkah panduan bergambar.
   - Pada langkah ke-5, klik **Selesaikan Perakitan** (sistem akan mengecek ketersediaan bahan baku dan memotong 10 unit Pelat Baja).
   - Di akhir proses, klik tombol **Unduh QR** untuk mengekspor gambar barcode unit ke galeri HP.
4. **Pengujian Kualitas (QC)**:
   - Di tab **Produksi** ➔ Klik **Pengujian Kualitas (QC)**.
   - Pilih unit dengan status `QC_PENDING` / `READY_FOR_RETEST` ➔ Klik **Uji Unit**.
   - Jawab 3 daftar pertanyaan evaluasi kelayakan:
     * **Lulus (QC Passed)**: Jika semua benar ➔ Status berubah menjadi `QC_PASSED`.
     * **Gagal (QC Rejected)**: Jika ada yang cacat ➔ Status menjadi `QC_REJECTED`. Unit yang gagal harus diperbaiki dengan mengklik **Perbaiki Unit** sehingga status naik ke `READY_FOR_RETEST` untuk diuji kembali.
5. **Pengemasan (Packing)**:
   - Di tab **Produksi** ➔ Klik **Kemas Unit (Packing)** (aktif jika ada unit `QC_PASSED`).
   - Masukkan ID Box, berat paket (kg), dan catatan visual pada dialog bottom sheet ➔ Klik **Konfirmasi Pengemasan** (status berubah menjadi `PACKED`).
6. **Aksi Cepat Scanner**:
   - Ketuk tombol FAB (+) di layar mana saja untuk membuka kamera pemindai. Arahkan ke QR Code unit untuk memicu jalan pintas pengujian QC atau pengemasan secara otomatis.

---

## 2. Peran: Staff Warehouse (Gudang & Logistik)

Staff Warehouse mengelola penerimaan bahan baku, peletakan barang jadi ke rak penyimpanan gudang, dan logistik outbound pengiriman barang.

### 🔐 Kredensial Login
* **Pilih Peran**: Warehouse (Tombol Tengah)
* **User ID**: `WH888`
* **Access Code**: `123456`

### 🔄 Alur Langkah Kerja
1. **Autentikasi**: Isi kredensial Warehouse dan klik **Authenticate**.
2. **Dasbor Stok**: Memantau saldo bahan baku. Jika persediaan Pelat Baja kritis (< 10 unit), panel dasbor akan menyala merah sebagai penanda reorder.
3. **Penerimaan Bahan Baku**:
   - Masuk ke tab **Gudang** ➔ Klik **Terima Bahan Baku**.
   - Masukkan detail supplier, nomor invoice, dan jumlah Qty Pelat Baja pada dialog bottom sheet ➔ Klik **Terima Barang** untuk menambah stok.
4. **Penyimpanan Barang Jadi (Gudang)**:
   - Masuk ke tab **Gudang** ➔ Lihat panel *Stok Siap Simpan* (aktif jika ada unit berstatus `PACKED`).
   - Klik **Simpan ke Gudang** ➔ Pilih rak peletakan (**Rak A1, A2, B1, atau B2**).
   - Klik **Simpan** ➔ Status unit menjadi `FINISHED` dan stok barang jadi bertambah 1 unit.
5. **Pengiriman Outbound**:
   - Di tab **Gudang** (bagian bawah) ➔ Pilih unit berstatus `FINISHED`.
   - Masukkan nama ekspedisi logistik dan nomor resi pengiriman ➔ Klik **Kirim Outbound** (status menjadi `SHIPPED` dan stok barang jadi berkurang).
6. **Registri QR**:
   - Masuk ke tab **Registri** untuk menyaring data unit atau mengunduh ulang gambar QR Code jika label cetak fisik pada paket rusak.

---

## 3. Peran: Plant Supervisor (Pengawas Shift)

Supervisor bertugas memantau seluruh proses operasional secara aman (Read-Only), melihat analitik statistik QC, dan mengekspor laporan shift.

### 🔐 Kredensial Login
* **Pilih Peran**: Supervisor (Tombol Kanan)
* **User ID**: `SP999`
* **Access Code**: `123456`

### 🔄 Alur Langkah Kerja
1. **Autentikasi**: Isi kredensial Supervisor dan klik **Authenticate**.
2. **Mode Read-Only**: Supervisor dapat masuk ke tab Produksi dan Gudang, namun seluruh tombol tindakan dikunci menjadi abu-abu redup (Read-Only) agar tidak mengubah data operasional berjalan.
3. **Ekspor Laporan**:
   - Di tab **Dashboard** ➔ Klik tombol **EKSPOR LAPORAN** di kanan atas.
   - Laporan shift berupa file teks disimpan otomatis ke memori internal HP pada folder:
     `Downloads/FabricationPro/laporan_shift_a.txt`
4. **Analitik Kualitas**:
   - Masuk ke tab **Monitoring** ➔ Memantau diagram batang perbandingan jumlah unit lulus QC vs gagal QC yang dihitung dinamis dari database Room.
5. **Inspeksi Registri**:
   - Masuk ke tab **Registri** untuk melakukan audit detail riwayat pendaftaran unit produk.
