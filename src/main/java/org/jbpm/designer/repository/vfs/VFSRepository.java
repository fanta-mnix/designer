package org.jbpm.designer.repository.vfs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.jbpm.designer.repository.Asset;
import org.jbpm.designer.repository.AssetBuilderFactory;
import org.jbpm.designer.repository.AssetNotFoundException;
import org.jbpm.designer.repository.Directory;
import org.jbpm.designer.repository.Filter;
import org.jbpm.designer.repository.Repository;
import org.jbpm.designer.repository.impl.AbstractAsset;
import org.jbpm.designer.repository.impl.AssetBuilder;
import org.jbpm.designer.web.profile.IDiagramProfile;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

public class VFSRepository implements Repository {

    private URI repositoryRoot;
    private Path repositoryRootPath;

    private FileSystem fileSystem;

    public VFSRepository(IDiagramProfile profile) {
        // TODO build env from profile params?
        this(profile, new HashMap<String, String>());
    }

    public VFSRepository(IDiagramProfile profile, Map<String, String> env) {
        this.repositoryRoot = URI.create(profile.getRepositoryRoot());

        this.fileSystem = FileSystems.getFileSystem( this.repositoryRoot );

        if ( fileSystem == null ) {

            try {
				this.fileSystem = FileSystems.newFileSystem( this.repositoryRoot, env );
			} catch (IOException e) {
				throw new AssertionError(e.getLocalizedMessage(), e);
			}
        }

        // fetch file system changes - mainly for remote based file systems
        String fetchCommand = (String) env.get("fetch.cmd");
        if (fetchCommand != null) {
            this.fileSystem = FileSystems.getFileSystem(URI.create(profile.getRepositoryRoot() + fetchCommand));
        }
        this.repositoryRootPath = fileSystem.provider().getPath(this.repositoryRoot);
    }
    
    public Collection<Directory> listDirectories(String startAt) {
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + startAt));
        DirectoryStream<Path> directories;
		try {
			directories = Files.newDirectoryStream(path, new DirectoryStream.Filter<Path>() {

			    public boolean accept( final Path entry ) throws IOException {
			        if ( Files.isDirectory(entry) ) {
			            return true;
			        }
			        return false;
			    }
			});
		} catch (IOException e) {
			return null;
		}
        Collection<Directory> foundDirectories = new ArrayList<Directory>();
        Iterator<Path> it = directories.iterator();
        while (it.hasNext()) {
            Path dir = it.next();
            String uniqueId = encodeUniqueId(dir.toUri().toString());
            foundDirectories.add(new Directory(uniqueId, dir.getFileName().toString(), trimLocation(dir)));
        }

        return foundDirectories;
    }

    public Collection<Asset> listAssetsRecursively(String startAt, final Filter filter) {
        final Collection<Asset> foundAssets = new ArrayList<Asset>();
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + startAt));

        try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			   public FileVisitResult visitFile(Path paths, BasicFileAttributes basicFileAttributes) throws IOException {
			       if (filter.accept(paths)) {
			           foundAssets.add(buildAsset(paths, false));
			       }
			       return FileVisitResult.CONTINUE;
			   }

			});
		} catch (IOException e) {
			return null;
		}

        return foundAssets;
    }

    public Directory createDirectory(String location) {
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + location));

        try {
			path = Files.createDirectories(path);
		} catch (IOException e) {
			return null;
		}
        String uniqueId = encodeUniqueId(path.toUri().toString());
        Directory directory = new Directory(uniqueId, path.getFileName().toString(), trimLocation(path));
        return directory;
    }

    public boolean directoryExists(String directory) {
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + directory));

        return Files.exists(path) && Files.isDirectory(path);
    }

    public boolean deleteDirectory(String directory, boolean failIfNotEmpty) {

        try {
            Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + directory));
            if (!Files.isDirectory(path)) {
                return false;
            }
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path paths, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(paths);

                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    if (e == null) {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }

            });

            return true;
        } catch (Exception e)  {
            return false;
        }
    }

    public boolean copyDirectory(String sourceDirectory, final String location) {
        if (!directoryExists(sourceDirectory)) {
            throw new IllegalArgumentException("Directory does not exist " + sourceDirectory);
        }
        try {

            final Path sourcePath = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + sourceDirectory));
            if (!Files.isDirectory(sourcePath)) {
                return false;
            }
            final String destinationPathRoot = getRepositoryRoot() + location + fileSystem.getSeparator() + sourcePath.getFileName().toString();
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path destinationPath = fileSystem.provider().getPath(URI.create(destinationPathRoot +
                            fileSystem.getSeparator() + sourcePath.relativize(dir)));
                    fileSystem.provider().createDirectory(destinationPath);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path currentFile, BasicFileAttributes basicFileAttributes) throws IOException {

                    if (!currentFile.endsWith(".gitignore")) {
                        Path destinationPath = fileSystem.provider().getPath(URI.create(destinationPathRoot +
                                fileSystem.getSeparator() + sourcePath.relativize(currentFile)));
                        createIfNotExists(destinationPath);

                        fileSystem.provider().copy(currentFile, destinationPath, null);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean moveDirectory(String sourceDirectory, final String location, String name) {
        if (!directoryExists(sourceDirectory)) {
            throw new IllegalArgumentException("Directory does not exist " + sourceDirectory);
        }
        try {
            final Path sourcePath = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + sourceDirectory));
            if (!Files.isDirectory(sourcePath)) {
                return false;
            }
            if (name == null) {
                name = sourcePath.getFileName().toString();
            }
            final String destinationPathRoot = getRepositoryRoot() + location + fileSystem.getSeparator() + name;

            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path currentFile, BasicFileAttributes basicFileAttributes) throws IOException {
                    Path destinationPath = fileSystem.provider().getPath(URI.create(destinationPathRoot
                            + fileSystem.getSeparator() + sourcePath.relativize(currentFile)));
                    createIfNotExists(destinationPath);
                    fileSystem.provider().move(currentFile, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    if (e == null) {
                        try {
                            Path destinationPath = fileSystem.provider().getPath(URI.create(destinationPathRoot
                                    + fileSystem.getSeparator() + sourcePath.relativize(dir)));
                            createIfNotExists(destinationPath);
                            fileSystem.provider().move(dir, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e1) {
                            fileSystem.provider().deleteIfExists(dir);
                        }
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }

            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Collection<Asset> listAssets(String location) {
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + location));
        DirectoryStream<Path> directories;
		try {
			directories = Files.newDirectoryStream(path, new DirectoryStream.Filter<Path>() {

			    public boolean accept( final Path entry ) throws IOException {
			        if (!Files.isDirectory(entry)) {
			            return true;
			        }
			        return false;
			    }
			});
		} catch (IOException e) {
			return null;
		}
        Collection<Asset> foundDirectories = new ArrayList<Asset>();
        Iterator<Path> it = directories.iterator();
        while (it.hasNext()) {
            Asset asset = buildAsset(it.next(), false);
            foundDirectories.add(asset);
        }

        return foundDirectories;
    }

    public Collection<Asset> listAssets(String location, final Filter filter) {
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + location));
        DirectoryStream<Path> directories;
		try {
			directories = Files.newDirectoryStream(path, new DirectoryStream.Filter<Path>() {

			    public boolean accept( final Path entry ) throws IOException {

			        return filter.accept(entry);
			    }
			});
		} catch (IOException e) {
			return null;
		}
        Collection<Asset> foundDirectories = new ArrayList<Asset>();
        Iterator<Path> it = directories.iterator();
        while (it.hasNext()) {
            Asset asset = buildAsset(it.next(), false);
            foundDirectories.add(asset);
        }

        return foundDirectories;
    }

    public Asset loadAsset(String assetUniqueId) throws AssetNotFoundException {
        String uniqueId = decodeUniqueId(assetUniqueId);
        Path assetPath = fileSystem.provider().getPath(URI.create(uniqueId));

        Asset asset = buildAsset(assetPath, true);

        return asset;
    }

    public Asset loadAssetFromPath(String location) throws AssetNotFoundException {
        Path path = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + location));

        if (Files.exists(path)) {
            return loadAsset(path.toUri().toString());
        } else {
            throw new AssetNotFoundException();
        }

    }

    public String createAsset(Asset asset) {
        Path filePath = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + (asset.getAssetLocation().equals("/")?"":asset.getAssetLocation()) + "/" +asset.getFullName()));
        createIfNotExists(filePath);
        try {
            OutputStream outputStream = fileSystem.provider().newOutputStream(filePath, StandardOpenOption.TRUNCATE_EXISTING);
            if(((AbstractAsset)asset).acceptBytes()) {
                outputStream.write(((Asset<byte[]>)asset).getAssetContent());
            } else {
                outputStream.write(asset.getAssetContent().toString().getBytes());
            }
            outputStream.close();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error when creating asset", e);
        }
        return encodeUniqueId(filePath.toUri().toString());
    }

    public String updateAsset(Asset asset) throws AssetNotFoundException {
        String uniqueId = decodeUniqueId(asset.getUniqueId());
        Path filePath = fileSystem.provider().getPath(URI.create(uniqueId));
        if (!Files.exists(filePath)) {
            throw new AssetNotFoundException();
        }
        try {
			if(((AbstractAsset)asset).acceptBytes()) {
			    Files.write(filePath, ((Asset<byte[]>)asset).getAssetContent(), StandardOpenOption.TRUNCATE_EXISTING);
			} else {
				Files.write(filePath, asset.getAssetContent().toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			}
		} catch (IOException e) {
			return null;
		}

        return asset.getUniqueId();
    }

    public boolean deleteAsset(String assetUniqueId) {
        String uniqueId = decodeUniqueId(assetUniqueId);
        try {
            return Files.deleteIfExists(fileSystem.provider().getPath(URI.create(uniqueId)));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAssetFromPath(String path) {

        Path filePath = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + path));

        return deleteAsset(filePath.toUri().toString());
    }

    public boolean assetExists(String assetUniqueId) {
        String uniqueId = decodeUniqueId(assetUniqueId);
        try {
            return Files.exists(fileSystem.provider().getPath(URI.create(uniqueId)));
        } catch (Exception e) {
            return Files.exists(fileSystem.provider().getPath(URI.create(getRepositoryRoot() + assetUniqueId)));
        }
    }

    public boolean copyAsset(String uniqueId, String location) {
        String decodedUniqueId = decodeUniqueId(uniqueId);
        if (!assetExists(decodedUniqueId)) {
            throw new IllegalArgumentException("Asset does not exist");
        }
        try {
            Path sourcePath = fileSystem.provider().getPath(URI.create(decodedUniqueId));
            Path destinationPath = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + location
                    + fileSystem.getSeparator() + sourcePath.getFileName().toString()));
            createIfNotExists(destinationPath);

            fileSystem.provider().copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean moveAsset(String uniqueId, String location, String name) {
        String decodedUniqueId = decodeUniqueId(uniqueId);
        if (!assetExists(decodedUniqueId)) {
            throw new IllegalArgumentException("Asset does not exist");
        }
        try {
            Path sourcePath = fileSystem.provider().getPath(URI.create(decodedUniqueId));
            if (name == null) {
                name = sourcePath.getFileName().toString();
            }

            Path destinationPath = fileSystem.provider().getPath(URI.create(getRepositoryRoot() + location + fileSystem.getSeparator() + name));
            createIfNotExists(destinationPath);
            fileSystem.provider().move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected Asset buildAsset(Path file, boolean loadContent) {

        String name = file.getFileName().toString();
        String location = trimLocation(file);

        AssetBuilder assetBuilder = AssetBuilderFactory.getAssetBuilder(name);
        BasicFileAttributes attrs;
		try {
			attrs = fileSystem.provider().readAttributes(file, BasicFileAttributes.class);
		} catch (IOException e1) {
			return null;
		}
        assetBuilder.uniqueId(encodeUniqueId(file.toUri().toString()))
                    .location(location)
                    .creationDate(attrs.creationTime() == null ? "" : new Date(attrs.creationTime().toMillis()).toString())
                    .lastModificationDate(attrs.lastModifiedTime() == null ? "" : new Date(attrs.lastModifiedTime().toMillis()).toString())
                    // TODO some provider specific details
                    .description("")
                    .owner("");

        if (loadContent) {
            try {
				if (((AbstractAsset)assetBuilder.getAsset()).acceptBytes()) {
				    assetBuilder.content(Files.readAllBytes(file));
				} else {
					List<String> lines = Files.readAllLines(file, Charsets.UTF_8);
				    assetBuilder.content(Joiner.on(System.lineSeparator()).join(lines));
				}
			} catch (IOException e) {
				return null;
			}
        }

        return assetBuilder.getAsset();
    }

    private String decodeUniqueId(String uniqueId) {
        if (Base64.isBase64(uniqueId)) {
            byte[] decoded = Base64.decodeBase64(uniqueId);
            try {
                return new String(decoded, "UTF-8");
            } catch (UnsupportedEncodingException e) {

            }
        }

        return uniqueId;
    }

    private String encodeUniqueId(String uniqueId) {
        try {
            return Base64.encodeBase64URLSafeString(uniqueId.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private String getRepositoryRoot() {
        String repo = this.repositoryRoot.toString();
        if (repo.endsWith("/")) {
            return repo.substring(0, repo.length()-2);
        }

        return repo;
    }

    private String trimLocation(Path file) {
        String location = "";
        String pathAsString =  file.getParent().toString();
        if(pathAsString.startsWith(this.repositoryRoot.getScheme())) {
            location = pathAsString.replaceFirst(getRepositoryRoot(), "");
        } else {
            location = pathAsString.replaceFirst(this.repositoryRootPath.toString(), "");
        }

        if (!location.startsWith(fileSystem.getSeparator())) {
            location = fileSystem.getSeparator() + location;
        }

        return location;
    }

    private void createIfNotExists(Path filePath) {
        if (!Files.exists(filePath.getParent())) {
            try {
                fileSystem.provider().createDirectory(filePath.getParent(), null);
            } catch (FileAlreadyExistsException e) {
                // TODO currently git provider does not properly check existence of directories
            } catch (IOException e) {
            	throw new AssertionError(e.getLocalizedMessage(), e);
			}
        }
    }
}
