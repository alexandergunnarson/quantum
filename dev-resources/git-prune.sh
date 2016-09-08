# Seek

git rev-list --objects --all | sort -k 2 > all-files-in-repo.txt &&
git gc && git verify-pack -v ../.git/objects/pack/pack-*.idx \
  | egrep "^\w+ blob\W+[0-9]+ [0-9]+ [0-9]+$"             \
  | sort -k 3 -n -r > big-objects.txt &&
join <(sort big-objects.txt) <(sort all-files-in-repo.txt) | \
  sort -k 3 -n -r | cut -f 1,3,6- -d \ > big-to-small.txt &&
git log --all --pretty=format: --name-only --diff-filter=D | \
  sed  's|[^/]+$||g' | \
  sort -u \
  > deleted-files.txt

# Destroy

echo "===== Pruning... =====" &&
    git filter-branch \
      --prune-empty -f \
      --index-filter  \
      'git rm -rf --cached --ignore-unmatch $FILES'  \
      --tag-name-filter cat -- --all &&
echo "===== Pruning done =====" &&
echo "===== Garbage collecting... =====" &&
    rm -rf .git/refs/original/ &&
    git reflog expire --expire=now --all &&
    git gc --aggressive --prune=now &&
echo "===== Garbage collecting done. =====" &&
echo "===== Pushing... =====" &&
    git push origin --force --all &&
    echo "===== Push origin done. =====" &&
    echo "===== Push tags... =====" &&
    git push origin --force --tags &&
    echo "===== Push tags done. =====" &&
echo "===== Pushing done. =====" &&
cd ../ &&
du -sh *

# Binaries:

# phantomjs
# fpcalc
# ffmpeg
# VLC
# MediaInfo
# x265