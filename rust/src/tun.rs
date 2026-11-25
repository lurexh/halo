use std::io;
use std::os::raw::c_int;

/// A layer-3 TUN device. On Android the fd comes from `VpnService.establish()`;
/// on Linux we open `/dev/net/tun`. All I/O goes through libc so it behaves the
/// same on both, and fd ownership stays clear of the Kotlin side.
pub struct Tun {
    fd: c_int,
    owned: bool,
}

impl Tun {
    pub fn from_fd(fd: c_int) -> Self {
        Self { fd, owned: false }
    }

    #[cfg(target_os = "linux")]
    pub fn open_linux(name: &str) -> io::Result<Self> {
        let fd = unsafe { libc::open(c"/dev/net/tun".as_ptr(), libc::O_RDWR) };
        if fd < 0 {
            return Err(io::Error::last_os_error());
        }
        let tun = Self { fd, owned: true };

        let mut ifr: libc::ifreq = unsafe { std::mem::zeroed() };
        let bytes = name.as_bytes();
        let n = bytes.len().min(libc::IFNAMSIZ - 1);
        for (i, byte) in bytes.iter().take(n).enumerate() {
            ifr.ifr_name[i] = *byte as libc::c_char;
        }
        ifr.ifr_ifru.ifru_flags = (libc::IFF_TUN | libc::IFF_NO_PI) as libc::c_short;
        let rc = unsafe { libc::ioctl(fd, libc::TUNSETIFF as libc::c_ulong, &ifr) };
        if rc < 0 {
            return Err(io::Error::last_os_error());
        }
        Ok(tun)
    }

    #[cfg(not(target_os = "linux"))]
    pub fn open_linux(_name: &str) -> io::Result<Self> {
        Err(io::Error::new(
            io::ErrorKind::Unsupported,
            "opening a named tun is only implemented on Linux",
        ))
    }

    /// Reads one packet, waiting at most 200 ms so callers can poll a stop flag.
    /// Returns `Ok(0)` on timeout.
    pub fn read_packet(&self, buf: &mut [u8]) -> io::Result<usize> {
        let mut set: libc::fd_set = unsafe { std::mem::zeroed() };
        unsafe {
            libc::FD_ZERO(&mut set);
            libc::FD_SET(self.fd, &mut set);
        }
        let mut timeout = libc::timeval {
            tv_sec: 0,
            tv_usec: 200_000,
        };
        let ready = unsafe {
            libc::select(
                self.fd + 1,
                &mut set,
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                &mut timeout,
            )
        };
        if ready < 0 {
            return Err(io::Error::last_os_error());
        }
        if ready == 0 {
            return Ok(0);
        }
        let n = unsafe {
            libc::read(self.fd, buf.as_mut_ptr() as *mut libc::c_void, buf.len())
        };
        if n < 0 {
            Err(io::Error::last_os_error())
        } else {
            Ok(n as usize)
        }
    }

    pub fn write_packet(&self, buf: &[u8]) -> io::Result<usize> {
        let mut written = 0usize;
        while written < buf.len() {
            let n = unsafe {
                libc::write(
                    self.fd,
                    buf[written..].as_ptr() as *const libc::c_void,
                    buf.len() - written,
                )
            };
            if n <= 0 {
                return Err(io::Error::last_os_error());
            }
            written += n as usize;
        }
        Ok(written)
    }
}

impl Drop for Tun {
    fn drop(&mut self) {
        if self.owned {
            unsafe {
                libc::close(self.fd);
            }
        }
    }
}
